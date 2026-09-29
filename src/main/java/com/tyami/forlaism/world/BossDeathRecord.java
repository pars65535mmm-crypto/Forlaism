package com.tyami.forlaism.world;

/**
 * ボスが受けた「即死」の記録。
 *
 * DPSと同じく、最後に記録してから一定tick経過すると自動的に消える。
 */
public final class BossDeathRecord {

    /** 検出した即死の種類。 */
    public enum Type {
        NONE("なし"),
        SET_HEALTH_ZERO("setHealth(0) 直叩き"),
        HURT_MAX_VALUE("hurt(Float.MAX)"),
        DIE_CALL("die() 直呼び"),
        KILL_CALL("kill() 直呼び"),
        DISCARD_CALL("discard() 直呼び"),
        REMOVE_CALL("remove() 直呼び"),
        SET_REMOVED_CALL("setRemoved() 直呼び"),
        ERASE_DETECTED("抹消系"),
        UNKNOWN("不明");

        private final String display;

        Type(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    /** 直近の即死タイプ。 */
    public Type lastType = Type.NONE;

    /** 直近の即死を呼び出した元（MOD名 or クラス名）。 */
    public String lastSource = "不明";

    /** 直近の即死の詳細（追加情報）。 */
    public String lastDetail = "";

    /** リセットまでの残りtick。 */
    public int displayTicks = 0;

    /** 表示保持tick（DPSと同じく20tick = 1秒）。 */
    public static final int DISPLAY_DURATION = 20;

    /**
     * 即死を記録する。
     */
    public void record(Type type, String source, String detail) {
        this.lastType = type;
        this.lastSource = source != null ? source : "不明";
        this.lastDetail = detail != null ? detail : "";
        this.displayTicks = DISPLAY_DURATION;
    }

    /**
     * 表示用のテキストを組み立てる。
     */
    public String buildDisplay() {
        if (lastType == Type.NONE || displayTicks <= 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("§c[即死: ").append(lastType.getDisplay());

        if (!lastSource.isEmpty() && !lastSource.equals("不明")) {
            sb.append(" §e@ ").append(lastSource);
        }

        if (!lastDetail.isEmpty()) {
            sb.append(" §7(").append(lastDetail).append(")");
        }

        sb.append("§c]");
        return sb.toString();
    }

    /** 表示が生きているか。 */
    public boolean isVisible() {
        return lastType != Type.NONE && displayTicks > 0;
    }

    /** リセット。 */
    public void clear() {
        lastType = Type.NONE;
        lastSource = "不明";
        lastDetail = "";
        displayTicks = 0;
    }
}