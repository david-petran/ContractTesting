package contracttesting;

import gherkin.deps.com.google.gson.*;
import io.swagger.models.*;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.properties.*;
import io.swagger.parser.SwaggerParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


public class ContractParser {

    Swagger swag;
    Map<String, Path> paths;
    Map<String, Parameter> params;
    Map<String, Response> responses;
    Map<String, Model> models;

    public ContractParser(String specFile) throws IOException{
        String swaggerString = new String(Files.readAllBytes(Paths.get(specFile)), StandardCharsets.UTF_8);
        swag = new SwaggerParser().parse(swaggerString);
        paths= swag.getPaths();
        params = swag.getParameters();
        responses = swag.getResponses();
        models = swag.getDefinitions();
    }

    private Model getModel(String model){
        return models.get(model.substring(model.lastIndexOf('/')+1));
    }

    public JsonObject getFullComparisonArray(String endpoint, String code){
        JsonObject derivedJson = new JsonObject();
        for (Map.Entry<String, Response> response: paths.get(endpoint).getGet().getResponses().entrySet()) {
            if (code.equals(response.getKey())) {
                derivedJson = (JsonObject)processProperty(getModel(response.getValue().getResponseSchema().getReference()), true);
            }
        }
        return derivedJson;
    }

    public JsonObject getRequiredComparisonArray(String endpoint, String code){
        JsonObject derivedJson = new JsonObject();
        for (Map.Entry<String, Response> response: paths.get(endpoint).getGet().getResponses().entrySet()) {
            if (code.equals(response.getKey())) {
                derivedJson = (JsonObject)processProperty(getModel(response.getValue().getResponseSchema().getReference()), false);
            }
        }
        return derivedJson;
    }


    private JsonElement processProperty(Property property, Boolean provideAllElements){
        JsonElement ret = null;
        if("string".equals(property.getType())){
            ret = processStr(property);
        } else if ("integer".equals(property.getType())){
            ret = processInt(property);
        } else if("boolean".equals(property.getType())){
            ret = processBool(property);
        } else if ("object".equals(property.getType())){
            ret = processObj(property, provideAllElements);
        } else if ("ref".equals(property.getType())){
            ret = processRef((RefProperty) property, provideAllElements);
        } else if ("array".equals(property.getType())){
            ret = processArr((ArrayProperty) property, provideAllElements);
        }
        return ret;
    }

    private JsonElement processProperty(Model model, Boolean provideAllElements){
        JsonElement ret = null;

        if(model instanceof ModelImpl){
            JsonObject obj = new JsonObject();
            for(Map.Entry<String, Property> objProp: ((ModelImpl)model).getProperties().entrySet()){
                if(propertyIsRequired(objProp.getValue(), provideAllElements)) {
                    obj.add(objProp.getKey(), processProperty(objProp.getValue(), provideAllElements));
                }
            }
            ret = obj;
        }else if (model instanceof ArrayModel){
            ret = new JsonArray();
            if(propertyIsRequired(((ArrayModel)model).getItems(), provideAllElements)){
                ((JsonArray)ret).add(processProperty (((ArrayModel)model).getItems(), provideAllElements));
            }
        }
        return ret;
    }

    private JsonElement processRef(RefProperty ref, Boolean provideAllElements){
        Model model = getModel(ref.get$ref());
        return processProperty(model, provideAllElements);
    }

    private JsonElement processObj(Property property, Boolean provideAllElements){
        JsonObject object = new JsonObject();
        if(property instanceof ObjectProperty){
            for(Map.Entry<String, Property> objProp: ((ObjectProperty)property).getProperties().entrySet()){
                if(propertyIsRequired(objProp.getValue(), provideAllElements)){
                    object.add(objProp.getKey(), processProperty(objProp.getValue(), provideAllElements));
                }
            }
        } else if (property instanceof ComposedProperty){
            for(Iterator<Property> iter = ((ComposedProperty) property).getAllOf().iterator(); iter.hasNext(); ){
                Property prop = iter.next();
                JsonObject combiObject = ((JsonObject)processProperty(prop, provideAllElements));
                if(propertyIsRequired(prop, provideAllElements)) {
                    for(Map.Entry<String, JsonElement> combi: combiObject.entrySet()){
                        object.add(combi.getKey(), combi.getValue());
                    }
                }
            }
        } else {
            return processProperty(property, provideAllElements);
        }
        return object;
    }

    private JsonElement processStr(Property property){
        return property == null ? JsonNull.INSTANCE : new JsonPrimitive("STRING");
    }

    private JsonElement processInt(Property property){
        return property == null ? JsonNull.INSTANCE : new JsonPrimitive("INTEGER");
    }

    private JsonElement processBool(Property property){
        return property == null ? JsonNull.INSTANCE : new JsonPrimitive("BOOLEAN");
    }

    private JsonArray processArr(ArrayProperty property, Boolean provideAllElements){
        JsonArray ret = new JsonArray();
        ret.add(processObj(property.getItems(), provideAllElements));
        return ret;
    }

    private boolean propertyIsRequired(Property property, boolean provideAllElements){
        return provideAllElements || "ref".equals(property.getType()) || property.getRequired();
    }


}
