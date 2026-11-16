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
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ArtAssetMapTest {
    private static final Path ART = Path.of("src", "main", "assets", "art");
    private static final Path SCENE_SOURCES = Path.of("..", "art-src", "scenes");
    private static final String[] MARINE_SOURCES = {
            "pink/Fishes2_17", "pink/Fishes2_14", "pink/Fishes2_3", "onepixel/mackerel",
            "pink/Fishes2_15", "pink/Fishes2_13", "pink/Fishes2_16", "elthen/octopus_0_0",
            "pink/Fishes2_11", "pink/Fishes2_12"
    };

    @Test
    public void everyCatalogEntryHasMatchingManifestAndNormalAndShinyImages() throws Exception {
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
            for (String suffix : new String[]{"", "_s"}) {
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

    @Test
    public void assetDirectoryContainsExactly42NormalAnd42IndependentShinyImages() throws Exception {
        List<String> filenames;
        try (Stream<Path> files = Files.list(ART)) {
            filenames = files.map(path -> path.getFileName().toString()).toList();
        }
        int speciesCount = GameProgress.ENTRY_COUNT / 2;
        assertEquals("Normal species images", speciesCount, filenames.stream()
                .filter(name -> name.matches("species_\\d{2}\\.png")).count());
        assertEquals("Shiny species images", speciesCount, filenames.stream()
                .filter(name -> name.matches("species_\\d{2}_s\\.png")).count());
        assertTrue("No bundled locked images", filenames.stream()
                .noneMatch(name -> name.startsWith("species_") && name.endsWith("_l.png")));

        for (int id = 0; id < speciesCount; id++) {
            Path normal = ART.resolve(String.format("species_%02d.png", id));
            Path shiny = ART.resolve(String.format("species_%02d_s.png", id));
            assertFalse("Shiny artwork must be an independent image: " + shiny.getFileName(),
                    java.util.Arrays.equals(Files.readAllBytes(normal), Files.readAllBytes(shiny)));
        }
    }

    @Test
    public void sixBundledScenesHaveEditableSourceProjects() throws Exception {
        List<String> sceneImages;
        try (Stream<Path> files = Files.list(ART)) {
            sceneImages = files.map(path -> path.getFileName().toString())
                    .filter(name -> name.matches("scene_\\d+\\.png")).toList();
        }
        assertEquals("Bundled scenes", 6, sceneImages.size());
        assertTrue("Scene source directory", Files.isDirectory(SCENE_SOURCES));
        try (Stream<Path> files = Files.list(SCENE_SOURCES)) {
            assertEquals("Editable scene projects", 6, files
                    .filter(path -> path.getFileName().toString().matches("scene_\\d+\\.json"))
                    .count());
        }

        for (int scene = 0; scene < 6; scene++) {
            Path image = ART.resolve(String.format("scene_%d.png", scene));
            Path source = SCENE_SOURCES.resolve(String.format("scene_%d.json", scene));
            assertTrue("Missing " + image.getFileName(), Files.isRegularFile(image));
            assertTrue("Missing editable source for scene " + scene, Files.isRegularFile(source));
            assertTrue("Empty source for scene " + scene, Files.size(source) > 0);
            assertTrue("Scene source must contain editable layers",
                    new String(Files.readAllBytes(source), StandardCharsets.UTF_8).contains("\"layers\""));
        }
    }

    @Test
    public void lockedImagesAreGeneratedAtRuntimeInsteadOfBundled() {
        for (int id = 0; id < GameProgress.ENTRY_COUNT / 2; id++) {
            Path legacy = ART.resolve(String.format("species_%02d_l.png", id));
            assertFalse("Redundant locked image remains: " + legacy.getFileName(), Files.exists(legacy));
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
