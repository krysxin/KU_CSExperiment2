package ch.idsia.agents;

import ch.idsia.benchmark.mario.engine.GlobalOptions;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.tasks.BasicTask;
import ch.idsia.benchmark.tasks.LearningTask;
import ch.idsia.tools.EvaluationInfo;
import ch.idsia.tools.MarioAIOptions;
import ch.idsia.utils.wox.serial.Easy;

public class LearningWithFPS implements LearningAgent {

	FPSAgent agent = new FPSAgent();

	String name = "LearningWithFPS";

	private FPSAgent bestAgent;

	private String args;

	// actionsのインデックス
	private static int actionIndex = 0;

	// ダメージを受けた時のインデックス
	private static int damagedIndex = 0;

	// 死んだ地点
	private static int deathPoint = 0;

	// 死んだ地点を記録する配列
	private int deathPoints2[] = new int[256];

	/* LearningWithFPSのコンストラクタ */
	public LearningWithFPS(String args) {
		agent = new FPSAgent();
		bestAgent = agent.clone();
		this.args = args;
	}

	public void learn() {

		MarioAIOptions marioAIOptions = new MarioAIOptions();
		BasicTask basicTask = new BasicTask(marioAIOptions);
		marioAIOptions.setArgs(args);

		/* プレイ画面出力するか否か */
		marioAIOptions.setVisualization(false);

		/* 学習が完了するまでwhile文の中をループ */
		int k = 0;
		while (true) {

			// 実行
			marioAIOptions.setAgent(agent);
			basicTask.setOptionsAndReset(marioAIOptions);
			if (!basicTask.runSingleEpisode(1)) {
				System.out.println("MarioAI: out of computational time" + " per action! Agent disqualified!");
			}

			// 情報を取得
			EvaluationInfo evaluationInfo = basicTask.getEvaluationInfo();

			
			// 死んだ地点を取得し記録する
			if (deathPoints2[deathPoint] != 0) {
				deathPoints2[deathPoint] += 1;
			} else {
				deathPoints2[deathPoint] = 1;
			}

			// 次の世代のagentを用意
			FPSAgent nextAgent = new FPSAgent();

			damagedIndex = agent.getDamagedIndex();
			actionIndex = agent.getActionIndex();

			// クリアしたらループを抜ける
			if (evaluationInfo.distancePassedCells >= 256 && evaluationInfo.marioMode == 2) {
				setNextAgent(agent, nextAgent, 1);
				agent = nextAgent.clone();
				bestAgent = agent.clone();
				break;
			}

			// 次のactionsを決定
			setNextAgent(agent, nextAgent, 0);

			// agentにコピー
			agent = nextAgent.clone();

			// 初期化
			damagedIndex = 0;
			actionIndex = 0;
			deathPoint = 0;

			k++;
			System.out.println("学習：" + k + "回目");
			System.out.println("distance：" + evaluationInfo.distancePassedCells);

		}

		// xmlで出力
		writeFile();

		// 確認のためbestAgentを実行
		marioAIOptions.setAgent(bestAgent);
		marioAIOptions.setVisualization(true);
		basicTask.setOptionsAndReset(marioAIOptions);
		if (!basicTask.runSingleEpisode(1)) {
			System.out.println("MarioAI: out of computational time" + " per action! Agent disqualified!");
		}

	}

	/*
	 * 次のactionsを決定する actionIndex：死んだ時のindex damagedIndex：ダメージを受けた時のindex
	 * deathPoint：死んだ地点
	 */
	private void setNextAgent(FPSAgent agent, FPSAgent nextAgent, int n) {

		// agentのactionsを取得
		byte[] actions = agent.getActions();

		// 巻き戻すインデックス
		int rewindIndex = deathPoints2[deathPoint] / 300 * 20; // 300回以上死んだら巻き戻す

		// クリアした時
		if (n == 1) {
			nextAgent.setActions(actions);
			return;
		}
		// ダメージを受けた時 //damagedIndex < actionIndex &&
		if ( damagedIndex != 0) {
			for (int i = damagedIndex - 10; i < actions.length; i++) {
				actions[i] = 0; // ダメージを受けたフレームの１０個前から以降のすべてのフレームでのactionをランダムに決める
			}
		}
		// 行き詰まった時
		if (agent.getIsDeadEnd()) {
			for (int i = agent.getDeadEndIndex() - 30; i < actions.length; i++) {
				actions[i] = 0;
			}
			
		}
		 //同じ場所で死に過ぎたらカウントを初期化
		 if (rewindIndex > actionIndex) {
		 deathPoints2[deathPoint]=1;
		 // 同じ場所で何回も死んだ時
		 } else if (rewindIndex >= 20) {
		 for (int i = actionIndex - rewindIndex; i < actions.length; i++) {
		 actions[i] = 0;
		 }
		 }
		

		// 上記以外の場合で死んだ時
		for (int i = actionIndex - 10; i < actions.length; i++) {
			actions[i] = 0;
		}
		// actionsをコピー
		nextAgent.setActions(actions);
	}

	/* xml作成 */
	private void writeFile() {
		String fileName = name + "-" + GlobalOptions.getTimeStamp() + ".xml";
		Easy.save(bestAgent, fileName);
	}

	@Override
	public void giveReward(float reward) {

	}

	@Override
	public void newEpisode() {

	}

	@Override
	public void setLearningTask(LearningTask learningTask) {

	}

	@Override
	public void setEvaluationQuota(long num) {

	}

	@Override
	public Agent getBestAgent() {
		return null;
	}

	@Override
	public void init() {

	}

	@Override
	public boolean[] getAction() {
		return new boolean[0];
	}

	@Override
	public void integrateObservation(Environment environment) {

	}

	@Override
	public void giveIntermediateReward(float intermediateReward) {

	}

	@Override
	public void reset() {

	}

	@Override
	public void setObservationDetails(int rfWidth, int rfHeight, int egoRow, int egoCol) {

	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public void setName(String name) {

	}
}
