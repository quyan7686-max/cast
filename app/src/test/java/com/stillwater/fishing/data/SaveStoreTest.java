package com.stillwater.fishing.data;

import android.content.SharedPreferences;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class SaveStoreTest {
    // In-memory preferences test double: apply atomically publishes the editor's pending values.
    private static final class MemoryPreferences {
        final Map<String, Object> values = new HashMap<>();
        int writes;
        final SharedPreferences preferences = (SharedPreferences) Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(), new Class<?>[]{SharedPreferences.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("edit")) return editor();
                    if (method.getName().startsWith("get") && args != null && args.length == 2) {
                        return values.getOrDefault(args[0], args[1]);
                    }
                    throw new UnsupportedOperationException(method.getName());
                });

        private SharedPreferences.Editor editor() {
            Map<String, Object> pending = new HashMap<>();
            return (SharedPreferences.Editor) Proxy.newProxyInstance(
                    SharedPreferences.Editor.class.getClassLoader(), new Class<?>[]{SharedPreferences.Editor.class},
                    (proxy, method, args) -> {
                        if (method.getName().startsWith("put")) {
                            pending.put((String) args[0], args[1]);
                            return proxy;
                        }
                        if (method.getName().equals("apply")) {
                            values.putAll(pending);
                            writes++;
                            return null;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
        }
    }

    @Test public void migratesAllLegacyRecordsWithoutChangingTheirMeaning() {
        MemoryPreferences memory = new MemoryPreferences();
        memory.values.put("scene", 5);
        memory.values.put("weatherHour", 12345L);
        memory.values.put("raining", true);
        for (int i = 0; i < 84; i++) {
            memory.values.put("count_" + i, i + 1);
            memory.values.put("length_" + i, Double.toString(i + .25));
            memory.values.put("weight_" + i, Double.toString(i + .5));
        }
        SaveStore store = new SaveStore(memory.preferences);
        GameProgress loaded = store.load();
        assertEquals(1, memory.values.get("schema_version"));
        assertEquals(1, memory.writes);
        assertEquals(5, loaded.sceneIndex);
        assertEquals(12345L, loaded.weatherHour);
        assertTrue(loaded.raining);
        for (int i = 0; i < 84; i++) {
            assertEquals(i + 1, loaded.entries[i].count);
            assertEquals(i + .25, loaded.entries[i].length, 0);
            assertEquals(i + .5, loaded.entries[i].weight, 0);
        }
        store.load();
        assertEquals("Migration must not run on every launch", 1, memory.writes);
    }

    @Test public void saveAndReloadKeepsQualitiesSeparate() {
        MemoryPreferences memory = new MemoryPreferences();
        SaveStore store = new SaveStore(memory.preferences);
        GameProgress progress = new GameProgress();
        progress.entry(29, true).count = 3;
        progress.entry(29, true).length = 77.25;
        progress.entry(29, true).weight = 1234.56;
        store.save(progress);
        GameProgress loaded = store.load();
        assertEquals(3, loaded.entry(29, true).count);
        assertEquals(77.25, loaded.entry(29, true).length, 0);
        assertEquals(1234.56, loaded.entry(29, true).weight, 0);
        assertFalse(loaded.entry(29, false).unlocked());
    }

    @Test public void invalidMeasurementsDoNotPoisonFutureRecordComparisons() {
        MemoryPreferences memory = new MemoryPreferences();
        memory.values.put("scene", 999);
        String[] invalid = {"NaN", "Infinity", "-Infinity", "invalid", "-8"};
        for (int i = 0; i < invalid.length; i++) {
            memory.values.put("count_" + i, 2);
            memory.values.put("length_" + i, invalid[i]);
            memory.values.put("weight_" + i, invalid[i]);
        }
        GameProgress loaded = new SaveStore(memory.preferences).load();
        assertEquals(Catalog.SCENES.length - 1, loaded.sceneIndex);
        for (int i = 0; i < invalid.length; i++) {
            assertEquals(2, loaded.entries[i].count);
            assertEquals(-1, loaded.entries[i].length, 0);
            assertEquals(-1, loaded.entries[i].weight, 0);
        }
    }

    @Test public void existingSpeciesIdsArePermanent() {
        String[] names = {"鲫鱼", "鲤鱼", "草鱼", "河蚌", "黑鱼", "罗非鱼", "泥鳅", "黄鳝",
                "鲶鱼", "黄颡鱼", "虾", "螃蟹", "青鱼", "白条鱼", "鳗鱼", "鳊鱼", "鳜鱼", "鲢鱼",
                "银鱼", "鳟鱼", "马口鱼", "鲈鱼", "鲻鱼", "沙丁鱼", "黄鱼", "带鱼", "鲳鱼", "鲭鱼",
                "河豚", "石斑鱼", "鲷鱼", "章鱼", "蛤蜊", "比目鱼", "空罐头", "木棍", "落叶", "破陶罐",
                "漂流瓶", "硬币", "小石头", "垃圾"};
        for (int i = 0; i < names.length; i++) {
            assertEquals(i, Catalog.get(i).id);
            assertEquals(names[i], Catalog.get(i).name);
        }
    }
}
