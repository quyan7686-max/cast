package com.stillwater.fishing.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.stillwater.fishing.data.Species.*;

public final class Catalog {
    public static final String[] SCENES = {"荷塘", "林间河流", "开阔湖泊", "山间溪流", "海港码头", "岩礁海岸"};
    private static final List<Species> ALL = new ArrayList<>();

    private static void fish(String name, int times, String weights, double l0, double l1, double w0, double w1) {
        add(name, false, times, weights, l0, l1, w0, w1);
    }

    private static void junk(String name, String weights, double l0, double l1, double w0, double w1) {
        add(name, true, ALL_DAY, weights, l0, l1, w0, w1);
    }

    private static void add(String name, boolean isJunk, int times, String source,
                            double l0, double l1, double w0, double w1) {
        int[] weights = new int[6];
        if (!source.isEmpty()) {
            String[] parts = source.split(",");
            for (int i = 0; i < 6; i++) weights[i] = Integer.parseInt(parts[i]);
        }
        ALL.add(new Species(ALL.size(), name, isJunk, times, weights, l0, l1, w0, w1));
    }

    static {
        fish("鲫鱼", ALL_DAY, "18,0,0,0,0,0", 8,48,8,2250);
        fish("鲤鱼", ALL_DAY, "15,14,0,0,0,0", 12,108,25,18000);
        fish("草鱼", DAWN|DAY, "14,0,0,0,0,0", 15,132,35,27000);
        fish("河蚌", ALL_DAY, "10,0,12,0,0,0", 4,26.4,15,1500);
        fish("黑鱼", ALL_DAY, "12,10,0,0,0,0", 12,90,20,15000);
        fish("罗非鱼", DAWN|DAY, "12,0,14,0,0,0", 8,55,10,4000);
        fish("泥鳅", DUSK|NIGHT, "10,10,0,0,0,0", 5,35,1,250);
        fish("黄鳝", NIGHT, "9,0,0,0,0,0", 12,100,4,1500);
        fish("鲶鱼", NIGHT, "0,10,0,0,0,0", 15,120,25,15000);
        fish("黄颡鱼", DUSK|NIGHT, "0,10,0,0,0,0", 8,36,8,450);
        fish("虾", ALL_DAY, "0,12,0,20,10,0", 2,18,.2,60);
        fish("螃蟹", DUSK|NIGHT, "0,8,0,15,8,12", 3,14.4,10,750);
        fish("青鱼", ALL_DAY, "0,8,12,0,0,0", 15,150,50,45000);
        fish("白条鱼", ALL_DAY, "0,12,0,20,0,0", 5,30,1,220);
        fish("鳗鱼", NIGHT, "0,6,0,0,6,0", 15,140,8,9000);
        fish("鳊鱼", DAWN|DAY, "0,0,18,0,0,0", 10,60,20,4500);
        fish("鳜鱼", NIGHT, "0,0,12,0,0,0", 12,72,25,6750);
        fish("鲢鱼", ALL_DAY, "0,0,18,0,0,0", 15,120,35,21000);
        fish("银鱼", DAWN|DAY, "0,0,14,0,0,0", 3,18,.1,20);
        fish("鳟鱼", DAWN|DAY, "0,0,0,20,0,0", 12,84,20,7500);
        fish("马口鱼", ALL_DAY, "0,0,0,25,0,0", 5,26.4,1,225);
        fish("鲈鱼", DAWN|DUSK, "0,0,0,0,12,0", 15,108,30,15000);
        fish("鲻鱼", ALL_DAY, "0,0,0,0,12,0", 12,84,15,6750);
        fish("沙丁鱼", DAWN|DAY, "0,0,0,0,12,0", 8,33.6,3,270);
        fish("黄鱼", DAWN|DUSK, "0,0,0,0,9,0", 10,90,10,10000);
        fish("带鱼", NIGHT, "0,0,0,0,8,0", 25,180,15,6500);
        fish("鲳鱼", ALL_DAY, "0,0,0,0,8,0", 10,70,20,7000);
        fish("鲭鱼", DAWN|DAY, "0,0,0,0,9,14", 12,75,15,6000);
        fish("河豚", ALL_DAY, "0,0,0,0,6,8", 8,65,15,6000);
        fish("石斑鱼", DUSK|NIGHT, "0,0,0,0,0,12", 15,120,50,30000);
        fish("鲷鱼", ALL_DAY, "0,0,0,0,0,16", 12,90,30,10500);
        fish("章鱼", NIGHT, "0,0,0,0,0,10", 10,120,20,7500);
        fish("蛤蜊", ALL_DAY, "0,0,0,0,0,16", 1.5,12,2,300);
        fish("比目鱼", DUSK|NIGHT, "0,0,0,0,0,12", 12,100,25,16000);
        junk("空罐头", "16,16,16,16,16,16", 5,21.6,8,180);
        junk("木棍", "16,16,16,16,16,16", 8,96,5,1800);
        junk("落叶", "14,14,14,14,14,14", 2,36,.05,22.5);
        junk("破陶罐", "10,10,10,10,10,10", 5,36,30,3750);
        junk("漂流瓶", "10,10,10,10,10,10", 8,42,50,1350);
        junk("硬币", "10,10,10,10,10,10", 1.5,4.8,1,45);
        junk("小石头", "14,14,14,14,14,14", 1,9.6,1.2,1050);
        junk("垃圾", "10,10,10,10,10,10", 10,72,.5,52.5);
    }

    private Catalog() {}

    public static List<Species> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static Species get(int id) {
        return ALL.get(id);
    }

    public static int period(int hour) {
        if (hour >= 5 && hour < 8) return 0;
        if (hour >= 8 && hour < 17) return 1;
        if (hour >= 17 && hour < 19) return 2;
        return 3;
    }

    public static String periodName(int hour) {
        return new String[]{"清晨", "白天", "黄昏", "夜晚"}[period(hour)];
    }

    public static String sceneText(Species species) {
        if (species.junk) return String.join("、", SCENES);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < SCENES.length; i++) {
            if (species.weights[i] > 0) {
                if (text.length() > 0) text.append("、");
                text.append(SCENES[i]);
            }
        }
        return text.toString();
    }
}
