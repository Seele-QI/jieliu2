package com.oculix.douyin.agent;

import com.oculix.douyin.util.AppLogger;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.util.Random;

public class ActionExecutor {
    private static ActionExecutor instance;
    private java.awt.Robot robot;
    private final Random random = new Random();
    private volatile boolean paused = false;

    private ActionExecutor() {
        try {
            robot = new java.awt.Robot();
            robot.setAutoDelay(1);
        } catch (Exception e) {
            AppLogger.error("\u521d\u59cb\u5316 Robot \u5931\u8d25: " + e.getMessage());
        }
    }

    public static synchronized ActionExecutor getInstance() {
        if (instance == null) instance = new ActionExecutor();
        return instance;
    }

    public void setPaused(boolean p) { this.paused = p; }

    public boolean execute(VisionAction action) {
        if (paused || robot == null) return false;
        try {
            switch (action.type) {
                case "click": return doClick(action.x, action.y);
                case "double_click": return doDoubleClick(action.x, action.y);
                case "right_click": return doRightClick(action.x, action.y);
                case "type": return doTypeWithPaste(action.text);
                case "scroll": return doScroll(action.amount);
                case "move": return doMove(action.x, action.y);
                case "hover": return doHover(action.x, action.y, 800 + random.nextInt(400));
                case "wait":
                    Thread.sleep(action.delayMs > 0 ? action.delayMs : 2000);
                    return true;
                case "screenshot": return true;
                default:
                    AppLogger.warn("\u672a\u77e5\u64cd\u4f5c\u7c7b\u578b: " + action.type);
                    return false;
            }
        } catch (Exception e) {
            AppLogger.error("\u6267\u884c\u64cd\u4f5c\u5931\u8d25: " + e.getMessage());
            return false;
        }
    }

    private boolean doClick(int x, int y) {
        if (x < 0 || y < 0) return false;
        humanMoveMouse(x, y);
        sleep(100 + random.nextInt(200));
        robot.mousePress(KeyEvent.BUTTON1_DOWN_MASK);
        sleep(50 + random.nextInt(100));
        robot.mouseRelease(KeyEvent.BUTTON1_DOWN_MASK);
        sleep(200 + random.nextInt(300));
        AppLogger.info("\u70b9\u51fb (" + x + ", " + y + ")");
        return true;
    }

    private boolean doDoubleClick(int x, int y) {
        if (x < 0 || y < 0) return false;
        humanMoveMouse(x, y);
        sleep(100 + random.nextInt(150));
        for (int i = 0; i < 2; i++) {
            robot.mousePress(KeyEvent.BUTTON1_DOWN_MASK);
            sleep(30 + random.nextInt(50));
            robot.mouseRelease(KeyEvent.BUTTON1_DOWN_MASK);
            sleep(50 + random.nextInt(80));
        }
        AppLogger.info("\u53cc\u51fb (" + x + ", " + y + ")");
        return true;
    }

    private boolean doRightClick(int x, int y) {
        if (x < 0 || y < 0) return false;
        humanMoveMouse(x, y);
        sleep(100 + random.nextInt(200));
        robot.mousePress(KeyEvent.BUTTON3_DOWN_MASK);
        sleep(50 + random.nextInt(100));
        robot.mouseRelease(KeyEvent.BUTTON3_DOWN_MASK);
        return true;
    }

    private boolean doTypeWithPaste(String text) {
        if (text == null || text.isEmpty()) return true;
        String display = text.length() > 30 ? text.substring(0, 30) + "..." : text;
        AppLogger.info("\u8f93\u5165\u6587\u672c (" + text.length() + " \u5b57): " + display);
        try {
            StringSelection selection = new StringSelection(text);
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(selection, null);
            sleep(100 + random.nextInt(100));
            robot.keyPress(KeyEvent.VK_CONTROL);
            sleep(30 + random.nextInt(50));
            robot.keyPress(KeyEvent.VK_V);
            sleep(50 + random.nextInt(80));
            robot.keyRelease(KeyEvent.VK_V);
            sleep(30 + random.nextInt(50));
            robot.keyRelease(KeyEvent.VK_CONTROL);
            sleep(300 + random.nextInt(500));
            AppLogger.info("\u6587\u672c\u5df2\u7c98\u8d34");
            return true;
        } catch (Exception e) {
            AppLogger.error("\u7c98\u8d34\u6587\u672c\u5931\u8d25: " + e.getMessage());
            return doTypeCharByChar(text);
        }
    }

    private boolean doTypeCharByChar(String text) {
        AppLogger.warn("\u4f7f\u7528\u9010\u5b57\u7b26\u8f93\u5165\uff08\u4ec5\u652f\u6301\u82f1\u6587/\u6570\u5b57/\u7b26\u53f7\uff09");
        for (char c : text.toCharArray()) {
            if (paused) break;
            if (c > 127) continue;
            int keyCode = charToKeyCode(c);
            if (keyCode != -1) {
                boolean shift = Character.isUpperCase(c) || "~!@#$%^&*()_+{}|:\"<>?".indexOf(c) >= 0;
                if (shift) robot.keyPress(KeyEvent.VK_SHIFT);
                robot.keyPress(keyCode);
                sleep(20 + random.nextInt(40));
                robot.keyRelease(keyCode);
                if (shift) robot.keyRelease(KeyEvent.VK_SHIFT);
            }
            sleep(30 + random.nextInt(60));
        }
        return true;
    }

    private boolean doScroll(int amount) {
        if (amount == 0) amount = -120;
        robot.mouseWheel(amount);
        sleep(200 + random.nextInt(300));
        AppLogger.info("\u6eda\u52a8: " + amount);
        return true;
    }

    private boolean doMove(int x, int y) {
        if (x < 0 || y < 0) return false;
        humanMoveMouse(x, y);
        return true;
    }

    private boolean doHover(int x, int y, int ms) {
        if (x < 0 || y < 0) return false;
        humanMoveMouse(x, y);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < ms) {
            if (paused) break;
            if (random.nextDouble() < 0.3) {
                robot.mouseMove(x + random.nextInt(5) - 2, y + random.nextInt(5) - 2);
            }
            sleep(150 + random.nextInt(200));
        }
        return true;
    }

    private void humanMoveMouse(int targetX, int targetY) {
        try {
            java.awt.Point current = java.awt.MouseInfo.getPointerInfo().getLocation();
            int startX = (int) current.getX();
            int startY = (int) current.getY();
            int distance = (int) Math.sqrt(Math.pow(targetX - startX, 2) + Math.pow(targetY - startY, 2));
            if (distance < 20) {
                robot.mouseMove(targetX, targetY);
                return;
            }
            int offset = Math.min(120, distance / 3);
            int ctrl1x = startX + (targetX - startX) / 3 + random.nextInt(offset * 2) - offset;
            int ctrl1y = startY + (targetY - startY) / 3 + random.nextInt(offset) - offset / 2;
            int ctrl2x = startX + 2 * (targetX - startX) / 3 + random.nextInt(offset * 2) - offset;
            int ctrl2y = startY + 2 * (targetY - startY) / 3 + random.nextInt(offset) - offset / 2;
            if (random.nextDouble() < 0.08) {
                ctrl1x += random.nextInt(120) - 60;
                ctrl1y += random.nextInt(80) - 40;
            }
            int steps = Math.max(15, distance / 40);
            for (int i = 0; i <= steps; i++) {
                if (paused) break;
                double t = (double) i / steps;
                t = easeInOutCubic(t);
                double mt = 1 - t;
                int x = (int) (mt*mt*mt * startX + 3*mt*mt*t * ctrl1x + 3*mt*t*t * ctrl2x + t*t*t * targetX);
                int y = (int) (mt*mt*mt * startY + 3*mt*mt*t * ctrl1y + 3*mt*t*t * ctrl2y + t*t*t * targetY);
                if (random.nextDouble() < 0.15) { x += random.nextInt(3) - 1; y += random.nextInt(3) - 1; }
                robot.mouseMove(x, y);
                double phase = Math.abs(t - 0.5) * 2;
                sleep((int)(4 + phase * 10 + random.nextInt(3)));
            }
        } catch (Exception e) {
            if (robot != null) robot.mouseMove(targetX, targetY);
        }
    }

    private double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    private int charToKeyCode(char c) {
        if (c >= 'a' && c <= 'z') return c - 'a' + KeyEvent.VK_A;
        if (c >= 'A' && c <= 'Z') return c - 'A' + KeyEvent.VK_A;
        if (c >= '0' && c <= '9') return c - '0' + KeyEvent.VK_0;
        switch (c) {
            case '\n': case '\r': return KeyEvent.VK_ENTER;
            case ' ': return KeyEvent.VK_SPACE;
            case '.': return KeyEvent.VK_PERIOD;
            case ',': return KeyEvent.VK_COMMA;
            case '-': return KeyEvent.VK_MINUS;
            case '/': return KeyEvent.VK_SLASH;
            case ';': return KeyEvent.VK_SEMICOLON;
            default: return -1;
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}