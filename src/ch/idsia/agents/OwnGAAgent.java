package ch.idsia.agents;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

import ch.idsia.agents.controllers.BasicMarioAIAgent;
import ch.idsia.benchmark.mario.engine.GeneralizerLevelScene;
import ch.idsia.benchmark.mario.engine.sprites.Mario;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.evolution.Evolvable;

/*
 * シフト演算子 x << y
 * xをyビットだけ左シフトする。空いた下位ビット(右側)には0を挿入。
 */
public class OwnGAAgent extends BasicMarioAIAgent implements Agent, Evolvable, Comparable, Cloneable {

	static String name = "GAAgent";

	/* 遺伝子情報 */
	public byte[] gene; // byte型配列

	/* 各個体の評価値保存用変数 */
	public int fitness;

	/* 環境から取得する入力数 */
	public int inputNum = 16;

	/* 乱数用変数 r */
	Random r = new Random();
	
	public int evaluation;

	private int over_brick_frame = 0; // brickの上にある
	private int total_upper_obstacle = 0; // 上の障害物を数える
	private int total_frame = 0; // すべてのobstacleの数

	public OwnGAAgent(int gene_size, String args) {
		super("OwnGAAgent");
		gene = new byte[(1 << inputNum)];
		loadFromFile(args);

		fitness = 0;

		over_brick_frame = 0;
		total_upper_obstacle = 0;
		total_frame = 0;
	}

	/* コンストラクタ */
	public OwnGAAgent() {
		super(name);
		
		if(overBrick()) {
			over_brick_frame++;
		}
		total_upper_obstacle += countUpperObstacle();
		total_frame ++ ;

		

		/* 16ビットの入力なので，65536(=2^16)個用意する */
		gene = new byte[(1 << inputNum)];

		/* 出力は32(=2^5)パターン */
		int num = 1 << (Environment.numberOfKeys - 1);

		int random;
		int flag = 1;

		/* geneの初期値は乱数(0から31)で取得 */
		for (int i = 0; i < gene.length; i++) {
			if (flag == 1) {
				switch (random = r.nextInt(8)) {
				case 0:
					gene[i] = 0;
					break;
				case 1:
					gene[i] = 2;
					break;
				case 2:
					gene[i] = 8;
					break;
				case 3:
					gene[i] = 10;
					break;
				case 4:
					gene[i] = 16;
					break;
				case 5:
					gene[i] = 18;
					break;
				case 6:
					gene[i] = 24;
					break;
				case 7:
					gene[i] = 26;
					break;

				}
			} else {
				gene[i] = (byte) r.nextInt(num);
			}

			// gene[i] = (byte)r.nextInt(17);
		}

		/* 評価値を0で初期化 */
		fitness = 0;

		over_brick_frame = 0;
		total_upper_obstacle = 0;
		total_frame = 0;

	}

	/* compfit()追加記述 */

	int distance;

	public void setFitness(int fitness) {
		this.fitness = fitness;
	}

	public int getFitness() {
		return fitness;
	}

	/* 降順にソート */
	public int compareTo(Object obj) {
		OwnGAAgent otherUser = (OwnGAAgent) obj;
		return -(this.fitness - otherUser.getFitness());
	}

	/* compFit()追加記述ここまで */

	/* ルールベース メソッド定義 */
	public boolean isGap(int r, int c) {

		for (int i = 0; i <= 9; i++) {
			if (getReceptiveFieldCellValue(r + i, c) != 0) {
				return false;
			}
		}
		return true;
	}

	public boolean isObstacle2(int r, int c) {
		return getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.BRICK
				|| getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.BORDER_CANNOT_PASS_THROUGH
				|| getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.FLOWER_POT_OR_CANNON
				|| getReceptiveFieldCellValue(r, c) == GeneralizerLevelScene.LADDER;
	}

	/* ルールベース メソッド定義ここまで */

	public boolean[] getAction() {

		int input = 0;

		/*
		 * 環境情報から input を決定 上位ビットから周辺近傍の状態を格納していく
		 */
		input += isEnemy(-1, -1) * (1 << 15);
		input += isEnemy(0, -1) * (1 << 14);
		input += isEnemy(1, -1) * (1 << 13);
		input += isEnemy(-1, 0) * (1 << 12);
		input += isEnemy(1, 0) * (1 << 11);
		input += isEnemy(-1, 1) * (1 << 10);
		input += isEnemy(1, 1) * (1 << 9);

		input += isObstacle(-1, -1) * (1 << 8);
		input += isObstacle(0, -1) * (1 << 7);
		input += isObstacle(1, -1) * (1 << 6);
		input += isObstacle(-1, 0) * (1 << 5);
		input += isObstacle(1, 0) * (1 << 4);
		input += isObstacle(-1, 1) * (1 << 3);
		input += isObstacle(1, 1) * (1 << 2);

		input += (isMarioOnGround ? 1 : 0) * (1 << 1);
		input += (isMarioAbleToJump ? 1 : 0) * (1 << 0);

		/* enemies情報(上位7桁) */
		/*
		 * input += probe(-1,-1,enemies) * (1 << 15); //probe * 2^15 input += probe(0
		 * ,-1,enemies) * (1 << 14); input += probe(1 ,-1,enemies) * (1 << 13); input +=
		 * probe(-1,0 ,enemies) * (1 << 12); input += probe(1 ,0 ,enemies) * (1 << 11);
		 * input += probe(-1,1 ,enemies) * (1 << 10); input += probe(1 ,1 ,enemies) * (1
		 * << 9); //遺伝子 キー 周辺状況から どのキーを決める 同じ環境に陥る
		 * 
		 * /* levelScene情報
		 */
		/*
		 * input += probe(1,-3,levelScene) * (1 << 16); input += probe(1,-2,levelScene)
		 * * (1 << 17); input += probe(1,1,levelScene) * (1 << 18);
		 * 
		 * 
		 * input += probe(-1,-1,levelScene) * (1 << 8); input += probe(0 ,-1,levelScene)
		 * * (1 << 7); input += probe(1 ,-1,levelScene) * (1 << 6); input += probe(-1,0
		 * ,levelScene) * (1 << 5); input += probe(1 ,0 ,levelScene) * (1 << 4); input
		 * += probe(-1,1 ,levelScene) * (1 << 3); input += probe(1 ,1 ,levelScene) * (1
		 * << 2);
		 * 
		 * input += (isMarioOnGround ? 1: 0) * (1 << 1); input += (isMarioAbleToJump ?
		 * 1: 0) * (1 << 0);
		 */

		// System.out.println("enemies : "+probe(1,0,enemies));

		/* input から output(act)を決定する */
		int act = gene[input]; // 遺伝子のinput番目の数値を読み取る
		for (int i = 0; i < Environment.numberOfKeys; i++) {
			action[i] = (act % 2 == 1); // 2で割り切れるならtrue
			act /= 2;
		}

		/* ルールベース */

		/*
		 * if (!action[Mario.KEY_LEFT]) { action[Mario.KEY_RIGHT] = true; }
		 */

		if (isGap(marioEgoRow, marioEgoCol + 2)) {
			action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
		}

		if (isObstacle2(marioEgoRow, marioEgoCol + 1) || isObstacle2(marioEgoRow, marioEgoCol + 2)) {
			action[Mario.KEY_JUMP] = isMarioAbleToJump || !isMarioOnGround;
		}

		
		return action;
	}

	/* 変更 */
	private double isEnemy(int x, int y) {
		int realX = x + 9;
		int realY = y + 9;
		return (getEnemiesCellValue(realX, realY) != 0) ? 1 : 0;
	}

	private double isObstacle(int x, int y) {
		int realX = x + 9;
		int realY = y + 9;
		return (getReceptiveFieldCellValue(realX, realY) < 0) ? 1 : 0;
	} // ここまで

	private double probe(int x, int y, byte[][] scene) {
		int realX = x + 11;
		int realY = y + 11;
		return (scene[realX][realY] != 0) ? 1 : 0;
	}

	public byte getGene(int i) {
		return gene[i];
	}

	public void setGene(int j, byte gene) {
		this.gene[j] = gene;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public int getDistance() {
		return distance;
	}
	
	public int getEvaluaiton() {
		return evaluation;
	}
	public void setEvaluaiton(int evaluation) {
		this.evaluation = evaluation;
	}
	
	private boolean OverBrick() {
		
	}
	
	private int countUpperObstacle() {
		
	}

	
	
	public void SaveToFile(String args) {
		
	}
	
	public void loadFromFile(String args) {
		
	}
	
	/* ここからファイルロード (途中結果）*/

	public static void saveLearningGene(OwnGAAgent[][] agents, String args) {
		try (FileOutputStream out = new FileOutputStream(convertFileName(args) + "gene")) {
			for (OwnGAAgent[] island: agents) { //島
			for (OwnGAAgent agent : island) {
				out.write(agent.gene);
			}
			}
			out.flush();
		} catch (IOException e) {
			System.err.println("error");
		}
	}

	public static void loadLearningGene(OwnGAAgent[][] agents, String args) {
		try (FileInputStream in = new FileInputStream(convertFileName(args) + "gene")) {
			for (OwnGAAgent[] island: agents) {
			for (OwnGAAgent agent : island) {
				in.read(agent.gene);
			}
			}
		} catch (FileNotFoundException e) {
			System.err.println("There is not file yet");
		} catch (IOException e) {
			System.err.println("error");
		}
	}

	public static String convertFileName(String args) {
		return args.replace(" ", " ").replace("-", "");
	}
	// ここまでファイルロード

	
	@Override
	public Evolvable getNewInstance() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	@Override
	public OwnGAAgent clone() {

		OwnGAAgent res = null;
		try {
			res = (OwnGAAgent) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new InternalError(e.toString());
		}

		return res;
	}

	@Override
	public void mutate() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public Evolvable copy() {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

}
