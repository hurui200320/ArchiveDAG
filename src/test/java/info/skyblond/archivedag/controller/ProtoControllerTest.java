package info.skyblond.archivedag.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import info.skyblond.archivedag.config.EmbeddedRedisConfiguration;
import info.skyblond.archivedag.config.ProtoProperties;
import info.skyblond.archivedag.model.ao.*;
import info.skyblond.archivedag.util.GeneralKt;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.*;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = EmbeddedRedisConfiguration.class)
@ActiveProfiles("test")
class ProtoControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    ProtoProperties protoProperties;
    @Autowired
    ProtoController protoController;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        this.objectMapper.findAndRegisterModules();
        this.mockMvc = MockMvcBuilders.standaloneSetup(this.protoController).build();
    }

    private AritegLinkModel[] parseLinkListResult(MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
        String content = result.getResponse().getContentAsString();
        return this.objectMapper.readValue(content, AritegLinkModel[].class);
    }

    private AritegLinkModel parseLinkResult(MvcResult result) throws UnsupportedEncodingException, JsonProcessingException {
        String content = result.getResponse().getContentAsString();
        return this.objectMapper.readValue(content, AritegLinkModel.class);
    }

    private File prepareTestFile(long size) throws IOException {
        File file = File.createTempFile(String.valueOf(System.currentTimeMillis()),
                String.valueOf(System.nanoTime()));
        byte[] buffer = new byte[4096]; // 4KB buffer
        try (
                var outputStream = new FileOutputStream(file)
        ) {
            long counter = 0L;
            Random random = new Random();
            while (counter < size) {
                random.nextBytes(buffer);
                outputStream.write(buffer, 0, (int) Math.min(buffer.length, size - counter));
                counter += buffer.length;
            }
        }
        file.deleteOnExit();
        return file;
    }

    private String uploadSimpleChunk() throws Exception {
        return this.parseLinkListResult(
                this.mockMvc.perform(multipart("/proto/chunk")
                                .file("files", new byte[1024]))
                        .andExpect(status().isOk())
                        .andReturn())[0].getLink();
    }

    @Test
    void testGetCommitNotFound() throws Exception {
        this.mockMvc.perform(get("/proto/commit")
                        .param("link", "W1kknXZLRvyN91meETWtiTKmiAYM4HNtyHekcEPZXYB8Tj"))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.emptyString()));
    }

    @Test
    void testGetCommitButBlob() throws Exception {
        this.mockMvc.perform(get("/proto/commit")
                        .param("link", this.uploadSimpleChunk()))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.emptyString()));
    }

    @Test
    void testCommit() throws Exception {
        var timestamp = GeneralKt.getUnixTimestamp();
        var sampleLink = this.uploadSimpleChunk();
        // create commit from null
        this.mockMvc.perform(post("/proto/commit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(new CreateCommitRequest(
                                "name", timestamp,
                                "message", null,
                                new SimplifiedLinkModel(sampleLink, "BLOB"),
                                new SimplifiedLinkModel(sampleLink, "BLOB")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", Matchers.equalTo("COMMIT")))
                .andDo(createFromNullResult -> {
                    // check the result
                    AritegLinkModel parentLink = this.parseLinkResult(createFromNullResult);
                    this.mockMvc.perform(get("/proto/commit")
                                    .param("link", parentLink.getLink()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.unix_timestamp", Matchers.equalTo((int) timestamp)))
                            .andExpect(jsonPath("$.commit_message", Matchers.equalTo("message")))
                            .andExpect(jsonPath("$.commit_link.link", Matchers.equalTo(sampleLink)))
                            .andExpect(jsonPath("$.author_link.link", Matchers.equalTo(sampleLink)))
                            .andExpect(jsonPath("$.parent_link", Matchers.nullValue()))
                            // create commit from existing parent
                            .andDo(queryResult -> this.mockMvc.perform(post("/proto/commit")
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(this.objectMapper.writeValueAsString(new CreateCommitRequest(
                                                    "name", timestamp,
                                                    "message",
                                                    new SimplifiedLinkModel(parentLink.getLink(), parentLink.getType()),
                                                    new SimplifiedLinkModel(sampleLink, "BLOB"),
                                                    new SimplifiedLinkModel(sampleLink, "BLOB")
                                            ))))
                                    .andExpect(status().isOk())
                                    .andExpect(jsonPath("$.type", Matchers.equalTo("COMMIT")))
                                    // check the parent link
                                    .andDo(it -> this.mockMvc.perform(get("/proto/commit")
                                                    .param("link", this.parseLinkResult(it).getLink()))
                                            .andExpect(status().isOk())
                                            .andExpect(jsonPath("$.unix_timestamp", Matchers.equalTo((int) timestamp)))
                                            .andExpect(jsonPath("$.commit_message", Matchers.equalTo("message")))
                                            .andExpect(jsonPath("$.commit_link.link", Matchers.equalTo(sampleLink)))
                                            .andExpect(jsonPath("$.author_link.link", Matchers.equalTo(sampleLink)))
                                            .andExpect(jsonPath("$.parent_link.link", Matchers.equalTo(parentLink.getLink())))));
                });
    }

    @Test
    void testRestore() throws Exception {
        this.mockMvc.perform(post("/proto/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(new RestoreRequest(List.of(
                                        this.uploadSimpleChunk(),
                                        "W1kknXZLRvyN91meETWtiTKmiAYM4HNtyHekcEPZXYB8Tj"
                                ), 0))
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", Matchers.equalTo(1)));
    }

    @Test
    void testGetStatusSample() throws Exception {
        String link = this.uploadSimpleChunk();
        this.mockMvc.perform(get("/proto/status")
                        .param("links", link))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$['" + link + "'].status",
                        Matchers.equalTo("ready")));
    }

    @Test
    void testGetStatusNotFound() throws Exception {
        this.mockMvc.perform(get("/proto/status")
                        .param("links", "W1kknXZLRvyN91meETWtiTKmiAYM4HNtyHekcEPZXYB8Tj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", Matchers.anEmptyMap()));
    }

    @Test
    void testGetSubLinksNotFound() throws Exception {
        this.mockMvc.perform(get("/proto/sub_link")
                        .param("link", "W1kknXZLRvyN91meETWtiTKmiAYM4HNtyHekcEPZXYB8Tj"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", Matchers.equalTo(0)));
    }

    @Test
    void testGetSubLinksBlob() throws Exception {
        this.mockMvc.perform(get("/proto/sub_link")
                        .param("link", this.uploadSimpleChunk()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", Matchers.equalTo(0)));
    }

    @Test
    void testGetSubLinksTree() throws Exception {
        this.mockMvc.perform(post("/proto/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(new CreateTreeRequest(
                                "name", List.of(new AritegLinkModel(
                                "sample", this.uploadSimpleChunk(), "BLOB")
                        )))))
                .andExpect(status().isOk())
                .andDo(mvcResult -> {
                    AritegLinkModel link = this.parseLinkResult(mvcResult);
                    this.mockMvc.perform(get("/proto/sub_link")
                                    .param("link", link.getLink()))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.length()", Matchers.equalTo(1)));
                });
    }

    @Test
    void testGetSubLinksList() throws Exception {
        try (
                var inputStream = new FileInputStream(this.prepareTestFile(
                        this.protoProperties.getBlobSize() * 3L * this.protoProperties.getListLength()))
        ) {
            this.mockMvc.perform(multipart("/proto/chunk")
                            .file(new MockMultipartFile("files", inputStream)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(Matchers.containsString("link")))
                    .andDo(mvcResult -> {
                        AritegLinkModel link = this.parseLinkListResult(mvcResult)[0];
                        this.mockMvc.perform(get("/proto/sub_link")
                                        .param("link", link.getLink()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()", Matchers.not(0)));
                    });
        }
    }

    @Test
    void testCreateTreeOK() throws Exception {
        var link = this.uploadSimpleChunk();
        this.mockMvc.perform(post("/proto/tree")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(this.objectMapper.writeValueAsString(new CreateTreeRequest("name",
                                List.of(new AritegLinkModel(
                                        "sample", link, "BLOB")
                                )))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type", Matchers.equalTo("TREE")));
    }

    @Test
    void testUploadZeroFile() throws Exception {
        this.mockMvc.perform(multipart("/proto/chunk"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testChunkConsistency() throws Exception {
        long size = this.protoProperties.getBlobSize() * 3L * this.protoProperties.getListLength();
        File file = this.prepareTestFile(size);
        byte[] content;
        try (
                var inputStream = new FileInputStream(file)
        ) {
            content = inputStream.readAllBytes();
        }

        this.mockMvc.perform(multipart("/proto/chunk")
                        .file(new MockMultipartFile("files", content)))
                .andExpect(status().isOk())
                .andExpect(content().string(Matchers.containsString("link")))
                .andDo(mvcResult -> {
                    AritegLinkModel link = this.parseLinkListResult(mvcResult)[0];
                    this.mockMvc.perform(get("/proto/chunk/name")
                                    .param("link", link.getLink()))
                            .andExpect(status().isOk())
                            .andExpect(content().bytes(content));
                });
    }

    @Test
    void testUploadFiles() {
        List.of(
                // Empty, small (in one blob)
                0L, this.protoProperties.getBlobSize() - 3L,
                // middle, in multiple blob
                this.protoProperties.getBlobSize() * 3L,
                // big, multiple list
                this.protoProperties.getBlobSize() * (1L + this.protoProperties.getListLength()),
                // large, two layer of list
                this.protoProperties.getBlobSize() * 3L * this.protoProperties.getListLength()
        ).forEach(size -> assertDoesNotThrow(() -> {
            File file = this.prepareTestFile(size);
            try (
                    InputStream inputStream = new FileInputStream(file)
            ) {
                this.mockMvc.perform(multipart("/proto/chunk")
                                .file(new MockMultipartFile("files", inputStream)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()", Matchers.greaterThan(0)));
            }
        }));
    }

    @Test
    void testDownloadChunkOkDefaultAttachment() throws Exception {
        this.mockMvc.perform(get("/proto/chunk/name")
                        .param("link", this.uploadSimpleChunk()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header()
                        .string(HttpHeaders.CONTENT_DISPOSITION,
                                Matchers.containsString("attachment;")));
    }

    @Test
    void testDownloadChunkOkWithType() throws Exception {
        this.mockMvc.perform(get("/proto/chunk/name")
                        .param("link", this.uploadSimpleChunk())
                        .param("type", "tYpE"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.header()
                        .string(HttpHeaders.CONTENT_DISPOSITION,
                                Matchers.containsString("tYpE;")));
    }

    @Test
    void testDownloadChunkMalformed() {
        assertThrows(Throwable.class, () ->
                this.mockMvc.perform(get("/proto/chunk/name")
                        .param("link", "MalFoRmeDlInk")));
    }

    @Test
    void testDownloadChunkNotFound() throws Exception {
        this.mockMvc.perform(get("/proto/chunk/name")
                        .param("link", "W1kknXZLRvyN91meETWtiTKmiAYM4HNtyHekcEPZXYB8Tj"))
                .andExpect(status().isNotFound());
    }

}
