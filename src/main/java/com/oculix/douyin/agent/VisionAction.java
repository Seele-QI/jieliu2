package com.oculix.douyin.agent;

/**
 * LLM ????????
 * type: click / double_click / right_click / type / scroll / move / hover / wait / screenshot
 * x, y: ????
 * text: ??????
 * amount: ???
 * delayMs: ?????
 * reasoning: LLM ?????
 */
public class VisionAction {
    public String type;
    public int x = -1;
    public int y = -1;
    public String text = "";
    public int amount = 0;
    public int delayMs = 2000;
    public String reasoning = "";

    public static VisionAction click(int x, int y, String reason) {
        VisionAction a = new VisionAction();
        a.type = "click"; a.x = x; a.y = y; a.reasoning = reason;
        return a;
    }

    public static VisionAction type(String text, String reason) {
        VisionAction a = new VisionAction();
        a.type = "type"; a.text = text; a.reasoning = reason;
        return a;
    }

    public static VisionAction scroll(int amount, String reason) {
        VisionAction a = new VisionAction();
        a.type = "scroll"; a.amount = amount; a.reasoning = reason;
        return a;
    }

    public static VisionAction wait(int ms, String reason) {
        VisionAction a = new VisionAction();
        a.type = "wait"; a.delayMs = ms; a.reasoning = reason;
        return a;
    }

    public static VisionAction screenshot() {
        VisionAction a = new VisionAction();
        a.type = "screenshot";
        return a;
    }

    public static VisionAction done(String reason) {
        VisionAction a = new VisionAction();
        a.type = "done"; a.reasoning = reason;
        return a;
    }

    public boolean isDone() { return "done".equals(type); }
}
