package com.datakomerz.pymes.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Provides custom Jackson serializers/deserializers for {@link PageImpl} so page results can be
 * stored inside Redis without losing pagination metadata.
 */
public class RedisPageModule extends SimpleModule {

  public RedisPageModule() {
    addDeserializer(PageImpl.class, new PageImplDeserializer());
  }

  private static class PageImplDeserializer extends JsonDeserializer<PageImpl<?>> {
    private static final TypeReference<List<Object>> CONTENT_TYPE = new TypeReference<>() {};

    @Override
    public PageImpl<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
      JsonNode node = p.getCodec().readTree(p);
      JsonNode contentNode = node.get("content");
      List<?> content = p.getCodec().readValue(contentNode.traverse(p.getCodec()), CONTENT_TYPE);
      JsonNode numberNode = node.has("pageNumber") ? node.get("pageNumber") : node.get("number");
      JsonNode sizeNode = node.has("pageSize") ? node.get("pageSize") : node.get("size");
      JsonNode totalNode = node.has("totalElements") ? node.get("totalElements") : node.get("total");
      int pageNumber = numberNode != null ? numberNode.asInt() : 0;
      int pageSize = sizeNode != null ? sizeNode.asInt() : content.size();
      long totalElements = totalNode != null ? totalNode.asLong() : content.size();
      PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
      return new PageImpl<>(content, pageRequest, totalElements);
    }
  }
}
