package com.oculix.douyin.core;

import com.oculix.douyin.model.Config;
import com.oculix.douyin.service.ConfigManager;
import com.oculix.douyin.util.AppLogger;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.Point;
import java.awt.MouseInfo;
import java.util.Random;

/**
 * ???????????????????????????
 *
 * Phase 2 ??????????????????????
 * ???????????????????
 */
public class AntiDetectionService {
    private static AntiDetectionService instance;
    private final Config config;
    private final Random random;
    private Robot robot;
    private volatile boolean paused = false;

    private AntiDetectionService() {
        this.config = ConfigManager.getInstance().getConfig();
        this.random = new Random();
        try {
            this.robot = new Robot();
            this.robot.setAutoDelay(1);
        } catch (AWTException e) {
            AppLogger.warn("Robot not available, mouse simulation disabled: " + e.getMessage());
        }
    }

    public static synchronized AntiDetectionService getInstance() {
        if (instance == null) {
            instance = new AntiDetectionService();
        }
        return instance;
    }

    // ======== ?????? ========

    /**
     * ????????????
     */
    public void randomDelay() {
        if (paused) return;
        int minDelay = config.getAntiDetection().getMinDelaySeconds();
        int maxDelay = config.getAntiDetection().getMaxDelaySeconds();
        int delay = minDelay + random.nextInt(maxDelay - minDelay + 1);
        AppLogger.info("Waiting " + delay + " seconds...");
        sleep(delay * 1000L);
    }

    /**
     * ?????????????
     */
    public void shortDelay() {
        if (paused) return;
        sleep(300 + random.nextInt(1200));
    }

    /**
     * ?????????????
     */
    public void microDelay() {
        if (paused) return;
        sleep(50 + random.nextInt(150));
    }

    /**
     * ???????????
     */
    public void delay(int minSeconds, int maxSeconds) {
        if (paused) return;
        int delay = minSeconds + random.nextInt(maxSeconds - minSeconds + 1);
        sleep(delay * 1000L);
    }

    // ======== ?????? Phase 2 ========

    /**
     * ??"??"????????????????????
     * ???????????????????
     */
    public void simulateViewing() {
        if (paused || robot == null) return;
        int viewingSeconds = 3 + random.nextInt(6); // 3-8 ?
        AppLogger.info("Viewing video for " + viewingSeconds + " seconds...");

        // ????????????
        long start = System.currentTimeMillis();
        long duration = viewingSeconds * 1000L;

        while (System.currentTimeMillis() - start < duration) {
            if (paused) break;

            int elapsed = (int)(System.currentTimeMillis() - start);
            int remaining = (int)(duration - elapsed);

            if (remaining <= 0) break;

            // 20% ???????
            if (random.nextDouble() < 0.2) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                int wiggleX = p.x + random.nextInt(20) - 10;
                int wiggleY = p.y + random.nextInt(15) - 7;
                robot.mouseMove(wiggleX, wiggleY);
            }

            // 10% ??????
            if (random.nextDouble() < 0.1) {
                robot.mouseWheel(random.nextInt(60) - 30);
            }

            // ??1-2?????
            int wait = Math.min(remaining, 800 + random.nextInt(1200));
            sleep(wait);
        }
    }

    /**
     * ??????????????????????????
     * ????????????/?????????
     */
    public void simulateHover(int x, int y, int hoverMs) {
        if (paused || robot == null) return;

        // ?????
        humanMoveMouse(x, y);

        // ????????
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < hoverMs) {
            if (paused) break;
            if (random.nextDouble() < 0.3) {
                int jx = x + random.nextInt(4) - 2;
                int jy = y + random.nextInt(4) - 2;
                robot.mouseMove(jx, jy);
            }
            sleep(200 + random.nextInt(300));
        }
    }

    /**
     * ??????/?????????????????
     * ??????????????/??
     */
    public void simulateBrowsingComments() {
        if (paused || robot == null) return;
        AppLogger.info("Browsing comments section...");

        // ???? 2-4 ?
        int scrollSegments = 2 + random.nextInt(3);
        for (int i = 0; i < scrollSegments; i++) {
            if (paused) break;

            int scrollAmount = 120 + random.nextInt(250);
            robot.mouseWheel(scrollAmount);
            sleep(800 + random.nextInt(2000));

            // ??????????
            if (random.nextDouble() < 0.25) {
                Point p = MouseInfo.getPointerInfo().getLocation();
                robot.mouseMove(p.x + random.nextInt(10) - 5, p.y + random.nextInt(8) - 4);
                sleep(300 + random.nextInt(700));
            }
        }

        // ????????
        sleep(1000 + random.nextInt(2000));
    }

    /**
     * ??????"??"??????????????
     */
    public void simulatePostCommentLoiter() {
        if (paused || robot == null) return;
        AppLogger.info("Loitering after comment...");

        sleep(1500 + random.nextInt(3000));

        // ???????
        Point p = MouseInfo.getPointerInfo().getLocation();
        int aimlessX = p.x + random.nextInt(40) - 20;
        int aimlessY = p.y + random.nextInt(30) - 15;
        humanMoveMouse(aimlessX, aimlessY);

        sleep(500 + random.nextInt(1500));

        // ???????
        if (random.nextDouble() < 0.3) {
            robot.mouseWheel(random.nextInt(120) - 60);
            sleep(800 + random.nextInt(1200));
        }
    }

    // ======== ?????? V2 ========

    /**
     * ??????????????? + ???? + ??? + ?????
     */
    public void humanMoveMouse(int targetX, int targetY) {
        if (paused || robot == null) return;

        try {
            Point current = MouseInfo.getPointerInfo().getLocation();
            int startX = (int) current.getX();
            int startY = (int) current.getY();

            // ????????????????
            int distance = (int) Math.sqrt(Math.pow(targetX - startX, 2) + Math.pow(targetY - startY, 2));
            if (distance < 30) {
                robot.mouseMove(targetX, targetY);
                return;
            }

            // ?????????????????????
            if (random.nextDouble() < 0.15) {
                int fakeX = startX + random.nextInt(60) - 30;
                int fakeY = startY + random.nextInt(40) - 20;
                moveAlongBezier(startX, startY, fakeX, fakeY, startX, startY, distance / 4);
                sleep(200 + random.nextInt(300));
                current = MouseInfo.getPointerInfo().getLocation();
                startX = (int) current.getX();
                startY = (int) current.getY();
            }

            int midX1, midY1, midX2, midY2;

            // ????????????????????
            int offset = Math.min(120, distance / 3);

            midX1 = startX + (targetX - startX) / 3 + random.nextInt(offset * 2) - offset;
            midY1 = startY + (targetY - startY) / 3 + random.nextInt(offset) - offset / 2;
            midX2 = startX + 2 * (targetX - startX) / 3 + random.nextInt(offset * 2) - offset;
            midY2 = startY + 2 * (targetY - startY) / 3 + random.nextInt(offset) - offset / 2;

            // ???????????
            if (random.nextDouble() < 0.08) {
                midX1 += random.nextInt(120) - 60;
                midY1 += random.nextInt(80) - 40;
                midX2 += random.nextInt(120) - 60;
                midY2 += random.nextInt(80) - 40;
            }

            int steps = calculateSteps(distance);
            moveBezierCubic(startX, startY, midX1, midY1, midX2, midY2, targetX, targetY, steps);

            // ?????????????
            if (random.nextDouble() < 0.35) {
                sleep(50 + random.nextInt(100));
                int jx = targetX + random.nextInt(4) - 2;
                int jy = targetY + random.nextInt(4) - 2;
                robot.mouseMove(jx, jy);
                sleep(30 + random.nextInt(60));
                robot.mouseMove(targetX, targetY);
            }

        } catch (Exception e) {
            if (robot != null) robot.mouseMove(targetX, targetY);
        }
    }

    /**
     * ????????????????P0?? P1 P2 P3????
     */
    private void moveBezierCubic(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, int steps) {
        double prevT = 0;
        for (int i = 0; i <= steps; i++) {
            if (paused) break;
            double t = (double) i / steps;

            // ???????????+????
            t = easeInOutCubic(t);

            // ????
            if (t < prevT) continue;
            prevT = t;

            double mt = 1 - t;
            int x = (int) (mt*mt*mt * x0 + 3*mt*mt*t * x1 + 3*mt*t*t * x2 + t*t*t * x3);
            int y = (int) (mt*mt*mt * y0 + 3*mt*mt*t * y1 + 3*mt*t*t * y2 + t*t*t * y3);

            // ???
            if (random.nextDouble() < 0.15) {
                x += random.nextInt(3) - 1;
                y += random.nextInt(3) - 1;
            }

            robot.mouseMove(x, y);

            // ???????????????????
            double phase = Math.abs(t - 0.5) * 2;
            int interval = (int)(4 + phase * 12 + random.nextInt(4));
            sleep(interval);
        }
    }

    /**
     * ?????????????????
     */
    private void moveAlongBezier(int... points) {
        int steps = points.length / 2 - 2;
        if (steps <= 0) return;
        int x0 = points[0], y0 = points[1];
        int x1 = points[2], y1 = points[3];
        int x2 = points[4], y2 = points[5];
        int x3 = points[6], y3 = points[7];
        moveBezierCubic(x0, y0, x1, y1, x2, y2, x3, y3, steps);
    }

    /**
     * ?????????????
     */
    private double easeInOutCubic(double t) {
        return t < 0.5 ? 4 * t * t * t : 1 - Math.pow(-2 * t + 2, 3) / 2;
    }

    /**
     * ????????????
     */
    private int calculateSteps(int distance) {
        int speed = 40 + random.nextInt(30); // ??/?
        return Math.max(15, distance / speed);
    }

    // ======== ?????? ========

    /**
     * ???????????
     */
    public void maybeScroll() {
        if (paused || robot == null) return;
        if (random.nextDouble() < 0.3) {
            int scrollAmount = 100 + random.nextInt(300);
            scrollAmount *= random.nextBoolean() ? 1 : -1;
            robot.mouseWheel(scrollAmount);
            AppLogger.info("Did random scroll: " + scrollAmount);
            shortDelay();
        }
    }

    /**
     * ?????????????
     */
    public void maybeRandomAction() {
        if (paused) return;
        if (random.nextDouble() < 0.1) {
            int x = 100 + random.nextInt(800);
            int y = 100 + random.nextInt(600);
            humanMoveMouse(x, y);
            AppLogger.info("Did random mouse wander");
        }
    }

    // ======== ?? ========

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
