package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.ai.dto.AiInsightsResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link AiAnalyticsResponseParser} — raw model output is never
 * trusted: malformed JSON degrades safely, enums are coerced, and the arrays are
 * bounded.
 */
@DisplayName("AiAnalyticsResponseParser")
class AiAnalyticsResponseParserTest {

    private final AiAnalyticsResponseParser parser = new AiAnalyticsResponseParser(new ObjectMapper());

    @Test
    @DisplayName("valid JSON → structured response with provider/model stamped")
    void validJson_parsed() {
        String raw = """
                {
                  "summary": "Revenue grew this period.",
                  "insights": [
                    {"type":"POSITIVE","title":"Revenue up","description":"Revenue rose to 1,000.","metric":"1,000","severity":"HIGH"}
                  ],
                  "recommendations": [
                    {"title":"Restock","description":"Restock low items.","priority":"MEDIUM"}
                  ]
                }
                """;

        AiInsightsResponse res = parser.parse(raw, "openai", "gpt-4o-mini");

        assertThat(res.available()).isTrue();
        assertThat(res.summary()).isEqualTo("Revenue grew this period.");
        assertThat(res.insights()).hasSize(1);
        assertThat(res.insights().get(0).type()).isEqualTo("POSITIVE");
        assertThat(res.insights().get(0).severity()).isEqualTo("HIGH");
        assertThat(res.recommendations()).hasSize(1);
        assertThat(res.recommendations().get(0).priority()).isEqualTo("MEDIUM");
        assertThat(res.provider()).isEqualTo("openai");
        assertThat(res.model()).isEqualTo("gpt-4o-mini");
        assertThat(res.generatedAt()).isNotNull();
    }

    @Test
    @DisplayName("not JSON → unavailable")
    void notJson_unavailable() {
        AiInsightsResponse res = parser.parse("I'm sorry, I cannot do that.", "openai", "m");
        assertThat(res.available()).isFalse();
        assertThat(res.insights()).isEmpty();
        assertThat(res.recommendations()).isEmpty();
        assertThat(res.provider()).isNull();
    }

    @Test
    @DisplayName("JSON array (not object) → unavailable")
    void jsonArray_unavailable() {
        assertThat(parser.parse("[1,2,3]", "openai", "m").available()).isFalse();
    }

    @Test
    @DisplayName("missing / blank summary → unavailable")
    void blankSummary_unavailable() {
        assertThat(parser.parse("{\"summary\":\"   \",\"insights\":[]}", "openai", "m").available()).isFalse();
        assertThat(parser.parse("{\"insights\":[]}", "openai", "m").available()).isFalse();
    }

    @Test
    @DisplayName("unknown enum values are coerced to safe defaults")
    void unknownEnums_coerced() {
        String raw = """
                {"summary":"ok","insights":[
                  {"type":"BANANA","title":"t","description":"d","metric":"","severity":"EXTREME"}
                ],"recommendations":[
                  {"title":"t","description":"d","priority":"URGENT"}
                ]}
                """;
        AiInsightsResponse res = parser.parse(raw, "p", "m");
        assertThat(res.insights().get(0).type()).isEqualTo("TREND");
        assertThat(res.insights().get(0).severity()).isEqualTo("LOW");
        assertThat(res.recommendations().get(0).priority()).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("more than 5 insights / recommendations are truncated")
    void arraysAreBounded() {
        StringBuilder ins = new StringBuilder();
        StringBuilder rec = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (i > 0) { ins.append(','); rec.append(','); }
            ins.append("{\"type\":\"TREND\",\"title\":\"t").append(i).append("\",\"description\":\"d\",\"metric\":\"\",\"severity\":\"LOW\"}");
            rec.append("{\"title\":\"t").append(i).append("\",\"description\":\"d\",\"priority\":\"LOW\"}");
        }
        String raw = "{\"summary\":\"ok\",\"insights\":[" + ins + "],\"recommendations\":[" + rec + "]}";
        AiInsightsResponse res = parser.parse(raw, "p", "m");
        assertThat(res.insights()).hasSize(5);
        assertThat(res.recommendations()).hasSize(5);
    }

    @Test
    @DisplayName("over-long description is capped at 1000 chars")
    void descriptionCapped() {
        String big = "x".repeat(5000);
        String raw = "{\"summary\":\"ok\",\"insights\":[{\"type\":\"TREND\",\"title\":\"t\",\"description\":\""
                + big + "\",\"metric\":\"\",\"severity\":\"LOW\"}]}";
        AiInsightsResponse res = parser.parse(raw, "p", "m");
        assertThat(res.insights().get(0).description().length()).isLessThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("insight with blank title and description is dropped")
    void blankInsightDropped() {
        String raw = "{\"summary\":\"ok\",\"insights\":[{\"type\":\"TREND\",\"title\":\"  \",\"description\":\"\",\"severity\":\"LOW\"}]}";
        assertThat(parser.parse(raw, "p", "m").insights()).isEmpty();
    }
}
