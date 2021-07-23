package com.mime.rpc.serializer.json;


import com.google.gson.*;
import com.google.gson.JsonSerializer;
import com.mime.rpc.enumeration.SerializerCode;
import com.mime.rpc.serializer.CommonSerializer;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

//参数序列化有些问题
public class GsonSerializer implements CommonSerializer {

    @Override
    public Object deserialize(byte[] bytes, Class<?> clazz) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
        String json = new String(bytes, StandardCharsets.UTF_8);
        return gson.fromJson(json, clazz);
    }

    @Override
    public int getCode() {
        return SerializerCode.valueOf("GSON").getCode();
    }

    @Override
    public byte[] serialize(Object object) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
        String json = gson.toJson(object);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    //gson需要特殊处理class类型 class->json的序列化操作    及json->class 的反序列化操作
    static class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                String str = json.getAsString();
                return Class.forName(str);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }

        @Override             //   String.class
        public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
            // class -> json
            return new JsonPrimitive(src.getName());
        }
    }
}
