package com.stillwater.fishing.ui;

import com.stillwater.fishing.data.Catalog;
import com.stillwater.fishing.data.GameProgress;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class ArtAssetMapTest {
    private static final Path ART = Path.of("src", "main", "assets", "art");
    private static final String[] MARINE_SOURCES = {
            "pink/Fishes2_17", "pink/Fishes2_14", "pink/Fishes2_3", "onepixel/mackerel",
            "pink/Fishes2_15", "pink/Fishes2_13", "pink/Fishes2_16", "elthen/octopus_0_0",
            "pink/Fishes2_11", "pink/Fishes2_12"
    };

    @Test
    public void everyCatalogEntryHasMatchingManifestAndThreeValidImages() throws Exception {
        List<String> lines = Files.readAllLines(ART.resolve("species_map.csv"), StandardCharsets.UTF_8);
        assertEquals("Header", "id,name,source", lines.get(0));
        assertEquals(GameProgress.ENTRY_COUNT / 2, lines.size() - 1);
        Set<Integer> ids = new HashSet<>();
        Set<String> names = new HashSet<>();

        for (int id = 0; id < GameProgress.ENTRY_COUNT / 2; id++) {
            String[] fields = lines.get(id + 1).split(",", -1);
            assertEquals("Three columns at row " + (id + 2), 3, fields.length);
            assertEquals("Contiguous ID", id, Integer.parseInt(fields[0]));
            assertEquals("Manifest name must match catalog", Catalog.get(id).name, fields[1]);
            assertFalse("Source is required", fields[2].isBlank());
            assertTrue("Duplicate ID", ids.add(id));
            assertTrue("Duplicate name", names.add(fields[1]));
            for (String suffix : new String[]{"", "_s", "_l"}) {
                String filename = String.format("species_%02d%s.png", id, suffix);
                Path image = ART.resolve(filename);
                assertTrue("Missing " + filename, Files.isRegularFile(image));
                assertPng96(image);
            }
        }
    }

    @Test
    public void marineSpeciesKeepTheirIntendedArtworkSources() throws Exception {
        List<String> lines = Files.readAllLines(ART.resolve("species_map.csv"), StandardCharsets.UTF_8);
        for (int id = 24; id <= 33; id++) {
            String[] fields = lines.get(id + 1).split(",", -1);
            assertEquals("Unexpected source for " + Catalog.get(id).name,
                    MARINE_SOURCES[id - 24], fields[2]);
        }
    }

    private static void assertPng96(Path image) throws IOException {
        byte[] header = new byte[24];
        try (InputStream input = Files.newInputStream(image)) {
            assertEquals("PNG header " + image.getFileName(), header.length, input.read(header));
        }
        assertEquals("PNG signature", 0x89, header[0] & 0xff);
        assertEquals('P', header[1]);
        assertEquals('N', header[2]);
        assertEquals('G', header[3]);
        assertEquals("Width " + image.getFileName(), 96, int32(header, 16));
        assertEquals("Height " + image.getFileName(), 96, int32(header, 20));
    }

    private static int int32(byte[] bytes, int offset) {
        return (bytes[offset] & 0xff) << 24 | (bytes[offset + 1] & 0xff) << 16
                | (bytes[offset + 2] & 0xff) << 8 | bytes[offset + 3] & 0xff;
    }
}
