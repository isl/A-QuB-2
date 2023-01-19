package forth.ics.isl.controller;

import org.springframework.context.annotation.ScopedProxyMode;

import java.io.IOException;
import java.sql.SQLException;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import com.fasterxml.jackson.databind.JsonNode;
import forth.ics.isl.data.model.EntityModel;
import forth.ics.isl.data.model.EntityTypes;
import forth.ics.isl.data.model.EntityTypes.EntityType;
import forth.ics.isl.data.model.parser.Utils;
import forth.ics.isl.service.BeautifyQueryResultsService;
import forth.ics.isl.service.PropertiesService;
import forth.ics.isl.triplestore.VirtuosoRestClient;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import timeprimitve.SISdate;

@Scope(scopeName = "session", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Controller
public class QueryController {

    @Value("${service.url}")
    private String serviceUrl;
    @Value("${triplestore.namespace}")
    private String namespace;
    private JsonNode currQueryResult;
    private VirtuosoRestClient restClient;

    // @Autowired
    private BeautifyQueryResultsService beautifyQueryResultsService;

    @PostConstruct
    public void init() throws IOException, SQLException {
    }

    @RequestMapping(value = "/final_search_query", method = RequestMethod.POST, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONObject searchEntityQuery(@RequestHeader(value = "Authorization") String authorizationToken,
            @RequestBody String queryModel) throws IOException {
        System.out.println("searchEntityQuery() in QueryController.java");
        System.out.println("QueryModel: " + queryModel);

        JSONObject jsonModel = Utils.parse(queryModel);
        String selectedGraphsClause = (String) jsonModel.get("queryFrom");
        JSONObject queryModelJson = (JSONObject) jsonModel.get("queryModel");
        JSONObject targetModel = (JSONObject) queryModelJson.get("targetModel");
        JSONArray relatedModels = (JSONArray) queryModelJson.get("relatedModels");

        JSONObject responseJsonObject = modelToSparql(targetModel, relatedModels, selectedGraphsClause);
        return responseJsonObject;
    }

    public JSONObject modelToSparql(JSONObject targetModel, JSONArray relatedModels, String selectedGraphsClause) {
        JSONObject responseJsonObject = new JSONObject();

        JSONObject firstRelatedModel = (JSONObject) relatedModels.get(0);
        JSONObject firstSelectedRelatedEntity = (JSONObject) firstRelatedModel.get("selectedRelatedEntity");

        if (firstSelectedRelatedEntity == null) {

            System.out.println(">> The user has NOT selected a related entity! ");
            String query = getQueryWithoutSelectedRelation(targetModel, selectedGraphsClause);
            System.out.println(">> Query: " + query);
            query = query.replaceAll("(\\.\\s*)+", ".");
            responseJsonObject.put("query", query);
        } else {
            System.out.println(">> The user has selected one or more related entities! ");

            int depthCounter = 1;

            EntityModel entityModel = new EntityModel((JSONObject) targetModel.get("selectedTargetEntity"));
            ArrayList<String> targetKeywords = getKeywords((JSONArray) targetModel.get("targetChips"));
            String targetFilterExpression = getFilterExpression(targetKeywords);
            JSONArray rowModelList = (JSONArray) firstRelatedModel.get("rowModelList");
            JSONObject targetRangeOfDates = (JSONObject) targetModel.get("rangeOfDates");
            String targetDateFrom = "";
            String targetDateUntil = "";
            if (targetRangeOfDates != null) {
                targetDateFrom = (String) targetRangeOfDates.get("from");
                targetDateUntil = (String) targetRangeOfDates.get("until");
            }
            JSONObject targetNumericRange = (JSONObject) targetModel.get("numericRange");
            double targetNumericFrom = -1;
            double targetNumericTo = -1;
            if (targetNumericRange != null) {

                if (targetNumericRange.get("from") != null) {
                    targetNumericFrom = Double.parseDouble(targetNumericRange.get("from").toString());
                }
                if (targetNumericRange.get("to") != null) {
                    targetNumericTo = Double.parseDouble(targetNumericRange.get("to").toString());
                }
            }

            JSONObject targetTimePrimitive = (JSONObject) targetModel.get("timePrimitive");

            String targetTimePrimitiveFrom = "";
            String targetTimePrimitiveTo = "";
            if (targetTimePrimitive.get("from") != null || targetTimePrimitive.get("to") != null) {

                if (targetTimePrimitive.get("from") != null) {
                    targetTimePrimitiveFrom = targetTimePrimitive.get("from").toString();
                }
                if (targetTimePrimitive.get("to") != null) {
                    targetTimePrimitiveTo = targetTimePrimitive.get("to").toString();
                }
            }

            JSONObject firstSelectedRelation = (JSONObject) firstRelatedModel.get("selectedRelation");

            ArrayList<String> firstRelatedKeywords = getKeywords((JSONArray) firstRelatedModel.get("relatedChips"));
            System.out.println("  >> First related keywords: " + firstRelatedKeywords);

            String firstRelatedFilterExpression = getFilterExpression(firstRelatedKeywords);
            System.out.println("  >> First related filter expression: " + firstRelatedFilterExpression);

            String firstRelationGraphPattern = (String) firstSelectedRelation.get("relation_graph_pattern");
            String firstSource_uri_variable = (String) firstSelectedRelation.get("source_uri_variable");
            String firstSource_shown_variables = (String) firstSelectedRelation.get("source_shown_variables");

            String firstDestination_uri_variable = (String) firstSelectedRelation.get("destination_uri_variable");
            String firstDestination_shown_variables = (String) firstSelectedRelation.get("destination_shown_variables");
            System.out.println(" firstDestination_shown_variables " + firstDestination_shown_variables);
            firstRelationGraphPattern = firstRelationGraphPattern.replace("[[i]]", "_" + String.valueOf(depthCounter));
            firstRelationGraphPattern = firstRelationGraphPattern.replace("[[j]]", "_" + String.valueOf(depthCounter));
            firstRelationGraphPattern = firstRelationGraphPattern.replace("[[k]]", "_" + String.valueOf(depthCounter));
            firstSource_uri_variable = firstSource_uri_variable.replace("[[i]]", "_" + String.valueOf(depthCounter));
            firstSource_shown_variables = firstSource_shown_variables.replace("[[i]]",
                    "_" + String.valueOf(depthCounter));
            firstDestination_uri_variable = firstDestination_uri_variable.replace("[[j]]",
                    "_" + String.valueOf(depthCounter));
            firstDestination_shown_variables = firstDestination_shown_variables.replace("[[j]]",
                    "_" + String.valueOf(depthCounter));

            String target_filter_pattern = "";

            if (!targetKeywords.isEmpty()) {
                System.out.println(">> Target Keywords is NOT empty");
                target_filter_pattern = (String) firstSelectedRelation.get("source_filter_pattern");
                target_filter_pattern = getFilterPattern(entityModel.getEntityType(), target_filter_pattern,
                        targetFilterExpression, depthCounter, depthCounter);

                if (!firstRelationGraphPattern.trim().endsWith(".")) {
                    firstRelationGraphPattern = firstRelationGraphPattern + " . " + target_filter_pattern;
                } else {
                    firstRelationGraphPattern = firstRelationGraphPattern + target_filter_pattern;
                }
            } else if (targetRangeOfDates != null) {
                System.out.println(">> Target date of range is NOT null");
                String expr = targetDateFrom + "##" + targetDateUntil;
                target_filter_pattern = (String) firstSelectedRelation.get("source_filter_pattern");
                target_filter_pattern = getFilterPattern(entityModel.getEntityType(), target_filter_pattern, expr,
                        depthCounter, depthCounter);

                if (!firstRelationGraphPattern.trim().endsWith(".")) {
                    firstRelationGraphPattern = firstRelationGraphPattern + " . " + target_filter_pattern;
                } else {
                    firstRelationGraphPattern = firstRelationGraphPattern + target_filter_pattern;
                }
            } else if (targetNumericFrom != -1 || targetNumericTo != -1) {
                System.out.println(">> Target numeric is NOT null");
                String expr = String.valueOf(targetNumericFrom) + "##" + String.valueOf(targetNumericTo);
                target_filter_pattern = (String) firstSelectedRelation.get("source_filter_pattern");
                target_filter_pattern = getFilterPattern(entityModel.getEntityType(), target_filter_pattern, expr,
                        depthCounter, depthCounter);

                if (!firstRelationGraphPattern.trim().endsWith(".")) {
                    firstRelationGraphPattern = firstRelationGraphPattern + " . " + target_filter_pattern;
                } else {
                    firstRelationGraphPattern = firstRelationGraphPattern + target_filter_pattern;
                }
            } else if (targetTimePrimitive.get("from") != null || targetTimePrimitive.get("to") != null) {

                System.out.println(">> Target time primitive  of range is NOT null");
                String expr = targetTimePrimitiveFrom + "##" + targetTimePrimitiveTo;
                target_filter_pattern = (String) firstSelectedRelation.get("source_filter_pattern");
                target_filter_pattern = getFilterPattern(entityModel.getEntityType(), target_filter_pattern, expr,
                        depthCounter, depthCounter);

                if (!firstRelationGraphPattern.trim().endsWith(".")) {
                    firstRelationGraphPattern = firstRelationGraphPattern + " . " + target_filter_pattern;
                } else {
                    firstRelationGraphPattern = firstRelationGraphPattern + target_filter_pattern;
                }
            }
            String firstSelectedRelatedEntityTypeS = (String) firstSelectedRelatedEntity.get("entityType");
            EntityType firstSelectedRelatedEntityType = EntityTypes.getEntityType(firstSelectedRelatedEntityTypeS);
            String destination_filter_pattern = (String) firstSelectedRelation.get("destination_filter_pattern");

            JSONObject firstRangeOfDates = (JSONObject) firstRelatedModel.get("rangeOfDates");
            JSONArray firstSelectedRelatedInstanceList = (JSONArray) firstRelatedModel
                    .get("selectedRelatedInstanceList");
            String firstDateFrom = "";
            String firstDateUntil = "";
            if (firstRangeOfDates != null) {
                firstDateFrom = (String) firstRangeOfDates.get("from");
                firstDateUntil = (String) firstRangeOfDates.get("until");
            }
            JSONObject firstBoundingBox = (JSONObject) firstRelatedModel.get("boundingBox");

            JSONObject firsttNumericRange = (JSONObject) firstRelatedModel.get("numericRange");
            double firstNumericFrom = -1;
            double firstNumericTo = -1;

            Boolean firstBooleanValues = (Boolean) firstRelatedModel.get("booleanValues");

            boolean flag = false;
            if (firsttNumericRange != null) {
                if (firsttNumericRange.get("from") != null) {
                    firstNumericFrom = Double.parseDouble(firsttNumericRange.get("from").toString());
                }
                if (firsttNumericRange.get("to") != null) {
                    firstNumericTo = Double.parseDouble(firsttNumericRange.get("to").toString());
                }
            }

            JSONObject firstTimePrimitiveRange = (JSONObject) firstRelatedModel.get("timePrimitive");
            System.out.println("firstTimePrimitiveRange::: " + firstTimePrimitiveRange);
            String firstTimePrimitiveFrom = "";
            String firstTimePrimitiveTo = "";

            if (firstTimePrimitiveRange != null) {
                if (firstTimePrimitiveRange.get("from") != null) {

                    firstTimePrimitiveFrom = firstTimePrimitiveRange.get("from").toString();
                }
                if (firstTimePrimitiveRange.get("to") != null) {
                    firstTimePrimitiveTo = firstTimePrimitiveRange.get("to").toString();
                }
            }

            String filterExpr = "";
            if ((!firstRelatedKeywords.isEmpty() || firstSelectedRelatedInstanceList.size() > 0)
                    && firstBoundingBox == null) {
                filterExpr = firstRelatedFilterExpression;
                if (firstSelectedRelatedInstanceList.size() > 0) {
                    filterExpr = "##" + getValues(firstSelectedRelatedInstanceList) + "##";
                }
                flag = true;

            } else if (firstDateFrom != "" || firstDateUntil != "") {
                filterExpr = firstDateFrom + "##" + firstDateUntil;
                flag = true;
            } else if (firstTimePrimitiveFrom != "" || firstTimePrimitiveTo != "") {
                filterExpr = firstTimePrimitiveFrom + "##" + firstTimePrimitiveTo;
                flag = true;
            } else if (firstBoundingBox != null) {
                String north = String.valueOf(firstBoundingBox.get("north"));
                String south = String.valueOf(firstBoundingBox.get("south"));
                String west = String.valueOf(firstBoundingBox.get("west"));
                String east = String.valueOf(firstBoundingBox.get("east"));

                filterExpr = "--" + north + "##" + south + "##" + east + "##" + west + "--";
                if (!firstRelatedKeywords.isEmpty()) {
                    if (firstSelectedRelatedInstanceList.size() > 0) {
                        filterExpr += "##" + getValues(firstSelectedRelatedInstanceList) + "##";
                    } else {
                        filterExpr += firstRelatedFilterExpression;
                    }
                }
                flag = true;

            } else if (firstNumericFrom != -1 || firstNumericTo != -1) {
                filterExpr = String.valueOf(firstNumericFrom) + "##" + String.valueOf(firstNumericTo);
                flag = true;
            } else if (firstBooleanValues != null) {
                filterExpr = firstBooleanValues.toString();
                flag = true;
            }

            if (flag) {
                destination_filter_pattern = getFilterPattern(firstSelectedRelatedEntityType,
                        destination_filter_pattern, filterExpr, depthCounter, depthCounter);
                if (!firstRelationGraphPattern.trim().endsWith(".")) {
                    firstRelationGraphPattern = firstRelationGraphPattern + " . " + destination_filter_pattern;
                } else {
                    firstRelationGraphPattern = firstRelationGraphPattern + destination_filter_pattern;
                }
            }

            if (!rowModelList.isEmpty()) {
                System.out.println(">> We have 'more' models...");
                String firstMoreGPs = getMoreGraphPatterns(rowModelList, depthCounter);
                System.out.println("  >> More Graph Pattern: " + firstMoreGPs);
                if (firstRelationGraphPattern.trim().endsWith(".")) {
                    firstRelationGraphPattern = firstRelationGraphPattern + firstMoreGPs;
                } else {
                    firstRelationGraphPattern = firstRelationGraphPattern + " . " + firstMoreGPs;
                }
            }

            if (relatedModels.size() == 1) {
                System.out.println(">> Related models == 1");
                StringBuilder queryBuilder = new StringBuilder("SELECT DISTINCT ");
                System.out.println("firstSource_shown_variables  " + firstSource_shown_variables
                        + " firstSource_uri_variable " + firstSource_uri_variable);
                if (!firstSource_shown_variables.contains(firstSource_uri_variable)) {
                    queryBuilder.append(firstSource_uri_variable).append(" ");
                }

                queryBuilder.append(firstSource_shown_variables).append(" ");
                if (!firstDestination_shown_variables.contains(firstDestination_uri_variable)) {
                    queryBuilder.append(firstDestination_uri_variable).append(" ");
                }

                queryBuilder.append(firstDestination_shown_variables);
                queryBuilder.append(" ").append(selectedGraphsClause);
                queryBuilder.append(" WHERE { ");
                queryBuilder.append(firstRelationGraphPattern);

                queryBuilder.append(" }");
                String query = queryBuilder.toString();
                // regex to replace . in sequence with spaces inside
                query = query.replaceAll("(\\.\\s*)+", ".");

                System.out.println(">> Query1: " + query);
                responseJsonObject.put("query", query);
            } else {
                System.out.println(">> Related models > 1");
                StringBuilder patternBuilder = new StringBuilder();
                ArrayList<String> selectVars = new ArrayList<>();
                if (!firstSource_shown_variables.contains(firstSource_uri_variable)) {
                    selectVars.add(firstSource_uri_variable);
                }
                selectVars.add(firstSource_shown_variables);
                if (!firstDestination_shown_variables.contains(firstDestination_uri_variable)) {
                    selectVars.add(firstDestination_uri_variable);
                }
                selectVars.add(firstDestination_shown_variables);

                int newIndex = depthCounter;
                for (int i = 1; i < relatedModels.size(); i++) {
                    newIndex++;

                    JSONObject otherRelatedModel = (JSONObject) relatedModels.get(i);
                    String otherExpression = (String) otherRelatedModel.get("outerSelectedFilterExpression");
                    JSONObject otherSelectedRelation = (JSONObject) otherRelatedModel.get("selectedRelation");
                    JSONObject otherSelectedRelatedEntity = (JSONObject) otherRelatedModel.get("selectedRelatedEntity");

                    String otherRelationGraphPattern = (String) otherSelectedRelation.get("relation_graph_pattern");
                    String otherDestination_uri_variable = (String) otherSelectedRelation
                            .get("destination_uri_variable");
                    String otherDestination_shown_variables = (String) otherSelectedRelation
                            .get("destination_shown_variables");

                    otherRelationGraphPattern = otherRelationGraphPattern.replace("[[i]]", "_" + depthCounter);
                    otherRelationGraphPattern = otherRelationGraphPattern.replace("[[j]]", "_" + newIndex);
                    otherRelationGraphPattern = otherRelationGraphPattern.replace("[[k]]", "_" + newIndex);
                    otherDestination_uri_variable = otherDestination_uri_variable.replace("[[j]]", "_" + newIndex);
                    otherDestination_shown_variables = otherDestination_shown_variables.replace("[[j]]",
                            "_" + newIndex);

                    if (otherExpression.equals("OR")) {
                        if (!target_filter_pattern.trim().equals("")) {
                            if (!otherRelationGraphPattern.trim().endsWith(".")) {
                                otherRelationGraphPattern = otherRelationGraphPattern + " . ";
                            }
                            otherRelationGraphPattern = otherRelationGraphPattern + target_filter_pattern;
                        }
                    }

                    ArrayList<String> otherRelatedKeywords = getKeywords(
                            (JSONArray) otherRelatedModel.get("relatedChips"));

                    String otherdestination_filter_pattern = (String) otherSelectedRelation
                            .get("destination_filter_pattern");
                    String otherSelectedRelatedEntityTypeS = (String) otherSelectedRelatedEntity.get("entityType");
                    EntityType otherSelectedRelatedEntityType = EntityTypes
                            .getEntityType(otherSelectedRelatedEntityTypeS);
                    JSONObject otherRangeOfDates = (JSONObject) otherRelatedModel.get("rangeOfDates");
                    String otherDateFrom = "";
                    String otherDateUntil = "";
                    if (otherRangeOfDates != null) {
                        otherDateFrom = (String) otherRangeOfDates.get("from");
                        otherDateUntil = (String) otherRangeOfDates.get("until");
                    }

                    JSONArray otherSelectedRelatedInstanceList = (JSONArray) otherRelatedModel
                            .get("selectedRelatedInstanceList");
                    JSONObject otherBoundingBox = (JSONObject) otherRelatedModel.get("boundingBox");

                    JSONObject othertNumericRange = (JSONObject) otherRelatedModel.get("numericRange");
                    double otherNumericFrom = -1;
                    double otherNumericTo = -1;
                    boolean otherFlag = false;
                    Boolean otherBooleanValues = (Boolean) otherRelatedModel.get("booleanValues");

                    if (othertNumericRange != null) {
                        if (othertNumericRange.get("from") != null) {
                            otherNumericFrom = Double.parseDouble(othertNumericRange.get("from").toString());
                        }
                        if (othertNumericRange.get("to") != null) {
                            otherNumericTo = Double.parseDouble(othertNumericRange.get("to").toString());
                        }
                    }

                    JSONObject otherRangeOfTimePrimitive = (JSONObject) otherRelatedModel.get("timePrimitive");
                    String otherTimePrimitiveFrom = "";
                    String otherTimePrimitiveTo = "";
                    if (otherRangeOfTimePrimitive != null) {
                        otherTimePrimitiveFrom = (String) otherRangeOfTimePrimitive.get("from");
                        otherTimePrimitiveTo = (String) otherRangeOfTimePrimitive.get("until");
                    }

                    String otherFilterExpr = "";

                    if ((!otherRelatedKeywords.isEmpty() || otherSelectedRelatedInstanceList.size() > 0)
                            && otherBoundingBox == null) {
                        otherFilterExpr = getFilterExpression(otherRelatedKeywords);
                        ;
                        if (otherSelectedRelatedInstanceList.size() > 0) {
                            otherFilterExpr = "##" + getValues(otherSelectedRelatedInstanceList) + "##";
                        }
                        otherFlag = true;
                    } else if (!otherDateFrom.equals("") || !otherDateUntil.equals("")) {
                        otherFilterExpr = otherDateFrom + "##" + otherDateUntil;
                        otherFlag = true;
                    } else if (!otherTimePrimitiveFrom.equals("") || !otherTimePrimitiveTo.equals("")) {
                        otherFilterExpr = otherTimePrimitiveFrom + "##" + otherTimePrimitiveTo;
                        otherFlag = true;
                    } else if (otherBoundingBox != null) {
                        String north = String.valueOf(firstBoundingBox.get("north"));
                        String south = String.valueOf(firstBoundingBox.get("south"));
                        String west = String.valueOf(firstBoundingBox.get("west"));
                        String east = String.valueOf(firstBoundingBox.get("east"));

                        otherFilterExpr = "--" + north + "##" + south + "##" + east + "##" + west + "--";

                        if (!otherRelatedKeywords.isEmpty()) {

                            if (otherSelectedRelatedInstanceList.size() > 0) {
                                otherFilterExpr += "##" + getValues(otherSelectedRelatedInstanceList) + "##";
                            } else {
                                otherFilterExpr += getFilterExpression(otherRelatedKeywords);
                            }
                        }
                        otherFlag = true;
                    } else if (otherNumericFrom != -1 || otherNumericTo != -1) {
                        otherFilterExpr = String.valueOf(otherNumericFrom) + "##" + String.valueOf(otherNumericTo);
                        otherFlag = true;
                    } else if (otherBooleanValues != null) {
                        otherFilterExpr = otherBooleanValues.toString();
                        otherFlag = true;
                    }

                    if (otherFlag) {
                        otherdestination_filter_pattern = getFilterPattern(otherSelectedRelatedEntityType,
                                otherdestination_filter_pattern, otherFilterExpr, depthCounter, newIndex);

                        if (!otherRelationGraphPattern.trim().endsWith(".")) {
                            otherRelationGraphPattern = otherRelationGraphPattern + " . ";
                        }
                        otherRelationGraphPattern = otherRelationGraphPattern + otherdestination_filter_pattern;
                    }

                    JSONArray otherRowModelList = (JSONArray) otherRelatedModel.get("rowModelList");
                    System.out.println("  >> Size of row model list: " + otherRowModelList.size());
                    if (!otherRowModelList.isEmpty()) {
                        String otherMoreGPs = getMoreGraphPatterns(otherRowModelList, newIndex);
                        System.out.println("  >> Other more Graph Pattern: " + otherMoreGPs);
                        if (otherRelationGraphPattern.trim().endsWith(".")) {
                            otherRelationGraphPattern = otherRelationGraphPattern + otherMoreGPs;
                        } else {
                            otherRelationGraphPattern = otherRelationGraphPattern + " . " + otherMoreGPs;
                        }
                    }

                    if (!selectVars.contains(otherDestination_uri_variable)) {
                        selectVars.add(otherDestination_uri_variable);
                    }

                    if (!selectVars.contains(otherDestination_shown_variables)) {
                        selectVars.add(otherDestination_shown_variables);
                    }
                    System.out.println("   > expression: " + otherExpression);
                    System.out.println("   > otherQueryGraphPattern: " + otherRelationGraphPattern);
                    System.out.println("   > Other related keywords: " + otherRelatedKeywords);

                    if (otherExpression.equals("AND")) {
                        if (i == 1) {
                            patternBuilder.append(firstRelationGraphPattern);
                        }

                        if (!firstRelationGraphPattern.trim().endsWith(".")) {
                            patternBuilder.append(" . ");
                        } else {
                            patternBuilder.append(" ");
                        }
                        patternBuilder.append(otherRelationGraphPattern);

                    } else {
                        if (i == 1) {
                            patternBuilder.append(" { ");
                            patternBuilder.append(firstRelationGraphPattern);
                            patternBuilder.append(" } ");
                        }
                        patternBuilder.append(" UNION { ");
                        patternBuilder.append(otherRelationGraphPattern);
                        patternBuilder.append(" } ");
                    }
                }

                StringBuilder queryBuilder = new StringBuilder("SELECT DISTINCT ");
                for (String var : selectVars) {
                    queryBuilder.append(var).append(" ");
                }
                queryBuilder.append(selectedGraphsClause);
                queryBuilder.append(" WHERE { ").append(patternBuilder.toString()).append(" }");
                String query = queryBuilder.toString();
                query = query.replaceAll("(\\.\\s*)+", ".");

                System.out.println(">> Query2: " + query);
                responseJsonObject.put("query", query);
            }

        }
        return responseJsonObject;
    }

    private String getQueryWithoutSelectedRelation(JSONObject targetModel, String selectedGraphsClause) {

        EntityModel entityModel = new EntityModel((JSONObject) targetModel.get("selectedTargetEntity"));
        ArrayList<String> targetKeywords = getKeywords((JSONArray) targetModel.get("targetChips"));
        System.out.println("---> targetKeywords = " + targetKeywords);
        String targetFilterExpression = getFilterExpression(targetKeywords);
        System.out.println("---> targetFilterExpression = " + targetFilterExpression);
        JSONObject targetRangeOfDates = (JSONObject) targetModel.get("rangeOfDates");
        String targetDateFrom = "";
        String targetDateUntil = "";

        if (targetRangeOfDates != null) {
            targetDateFrom = (String) targetRangeOfDates.get("from");
            targetDateUntil = (String) targetRangeOfDates.get("until");
        }

        JSONObject targetRangeOfTimePrimitive = (JSONObject) targetModel.get("timePrimitive");
        String targetTimePrimitiveFrom = "";
        String targetTimePrimitiveTo = "";

        if (targetRangeOfTimePrimitive != null) {
            targetTimePrimitiveFrom = (String) targetRangeOfTimePrimitive.get("from");
            targetTimePrimitiveTo = (String) targetRangeOfTimePrimitive.get("until");
        }

        JSONObject targetNumericRange = (JSONObject) targetModel.get("numericRange");
        double targetNumericFrom = -1;
        double targetNumericTo = -1;
        if (targetNumericRange != null) {
            if (targetNumericRange.get("from") != null) {
                targetNumericFrom = Double.parseDouble(targetNumericRange.get("from").toString());
            }
            if (targetNumericRange.get("to") != null) {
                targetNumericTo = Double.parseDouble(targetNumericRange.get("to").toString());
            }
        }

        String query = entityModel.getSearchQuery();
        query = query.replace("[[FROM_GRAPHS]]", selectedGraphsClause);

        int pos = query.indexOf("[[FILTER(");
        int posEnd = query.indexOf(")]]");
        if (pos != -1) {
            String filterPattern = query.substring(pos + 2, posEnd + 1);
            query = query.replace("[[" + filterPattern + "]]", "");
        }

        int posRegex = 0;
        int posEndRegex = 0;
        String literalPatternRegex = "";
        posRegex = query.indexOf("[[REGEX(");
        posEndRegex = query.indexOf(")]]");
        if (posRegex != -1) {
            literalPatternRegex = query.substring(posRegex + 2, posEndRegex + 1);
            query = query.replace("[[" + literalPatternRegex + "]]", "");
        }

        if (!targetKeywords.isEmpty()) {
            query = entityModel.getSearchQuery();
            query = query.replace("[[FROM_GRAPHS]]", selectedGraphsClause);
            query = getLiteralFilter(query, targetFilterExpression);

        } else if (targetRangeOfDates != null) {
            query = entityModel.getSearchQuery();
            query = query.replace("[[FROM_GRAPHS]]", selectedGraphsClause);
            query = getDateFilter(query, targetDateFrom, targetDateUntil);
        } else if (targetNumericFrom != -1 || targetNumericTo != -1) {
            query = entityModel.getSearchQuery();
            query = query.replace("[[FROM_GRAPHS]]", selectedGraphsClause);
            query = getNumericFilter(query, targetNumericFrom, targetNumericTo);
        } else if (targetTimePrimitiveFrom != null || targetTimePrimitiveTo != null) {
            query = entityModel.getSearchQuery();
            query = query.replace("[[FROM_GRAPHS]]", selectedGraphsClause);
            query = getTimePrimitiveFilter(query, targetTimePrimitiveFrom, targetTimePrimitiveTo);
        }

        return query;
    }

    private ArrayList<String> getKeywords(JSONArray targetChips) {
        ArrayList<String> keywords = new ArrayList<>();
        for (Object chip : targetChips) {
            JSONObject chipObj = (JSONObject) chip;
            String chipname = (String) chipObj.get("name");
            keywords.add(chipname);
        }

        return keywords;
    }

    private String getValues(JSONArray selectedRelatedInstanceList) {
        String expr = "(";
        for (int i = 0; i < selectedRelatedInstanceList.size(); i++) {
            JSONObject instance = (JSONObject) selectedRelatedInstanceList.get(i);
            System.out.println("instance:: " + instance);
            Iterator it = instance.keySet().iterator();
            String value = "";
            while (it.hasNext()) {
                String key = (String) it.next();
                String type = (String) ((JSONObject) instance.get(key)).get("type");
                if (type.equals("uri")) {
                    value = (String) ((JSONObject) instance.get(key)).get("value");
                    break;
                }
            }
            expr += "<" + value + ">";
            if (i < selectedRelatedInstanceList.size() - 1) {
                expr += ", ";
            }
        }
        expr += ")";
        return expr;
    }

    private String getDateFilter(String filterPattern, String dateFrom, String dateUntil) {
        int pos = filterPattern.indexOf("[[FILTER(");
        int posEnd = filterPattern.indexOf(")]]");

        String datePattern = filterPattern.substring(pos + 9, posEnd);
        String wholeDatePattern = filterPattern.substring(pos + 2, posEnd + 1);

        if ((dateFrom == null || dateFrom.equals("null") || dateFrom.contentEquals(""))
                && (dateUntil == null || dateUntil.equals("null") || dateUntil.equals(""))) {
            filterPattern = filterPattern.replace("[[" + wholeDatePattern + "]]", "");
        } else {
            boolean hasFrom = false;
            String newDatePattern = "";
            if (dateFrom != null && !dateFrom.equals("null") && !dateFrom.contentEquals("")) {
                newDatePattern = datePattern + ">=\"" + dateFrom + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>";
                hasFrom = true;
            }
            if (dateUntil != null && !dateUntil.equals("null") && !dateUntil.contentEquals("")) {
                if (hasFrom) {
                    newDatePattern += " AND ";
                }
                newDatePattern += datePattern + "<=\"" + dateUntil + "\"^^<http://www.w3.org/2001/XMLSchema#dateTime>";

            }
            String newWholeDatePattern = wholeDatePattern.replace(datePattern, newDatePattern);
            filterPattern = filterPattern.replace("[[" + wholeDatePattern + "]]", newWholeDatePattern);

        }
        return filterPattern;

    }

    private String getTimePrimitiveFilter(String filterPattern, String timeFrom, String timeTo) {
        int pos = filterPattern.indexOf("[[TIMEPRIM(");
        int posEnd = filterPattern.indexOf(")]]");
        String timePattern = filterPattern.substring(pos + 11, posEnd);
        String wholeTimePattern = filterPattern.substring(pos + 2, posEnd + 1);
        String bb = timePattern.split(",")[0];
        String ee = timePattern.split(",")[1];
        if ((timeFrom == null || timeFrom.equals("null") || timeFrom.contentEquals(""))
                && (timeTo == null || timeTo.equals("null") || timeTo.equals(""))) {
            filterPattern = filterPattern.replace("[[" + wholeTimePattern + "]]", "");
        } else {
            boolean hasFrom = false;
            String newTimePattern = "";
            if (timeFrom != null && !timeFrom.equals("null") && !timeFrom.contentEquals("")) {
                SISdate sisTime = new SISdate(timeFrom);
                int x1 = sisTime.getFrom();
                newTimePattern = "xsd:int(" + bb + ")" + ">=" + x1;
                hasFrom = true;
            }
            if (timeTo != null && !timeTo.equals("null") && !timeTo.contentEquals("")) {
                if (hasFrom) {
                    newTimePattern += " AND ";
                }
                SISdate sisTime = new SISdate(timeTo);
                int y2 = sisTime.getTo();
                newTimePattern += "xsd:int(" + ee + ")" + "<=" + y2;

            }

            String newWholeTimePattern = wholeTimePattern.replace(timePattern, newTimePattern);
            filterPattern = filterPattern.replace("[[" + wholeTimePattern + "]]", newWholeTimePattern);
            filterPattern = filterPattern.replace("TIMEPRIM", "FILTER ");
            System.out.println("filterPattern:::: " + filterPattern);
        }
        return filterPattern;

    }

    private String getNumericFilter(String filterPattern, double from, double to) {
        int pos = filterPattern.indexOf("[[FILTER(");
        int posEnd = filterPattern.indexOf(")]]");

        String numericPattern = filterPattern.substring(pos + 9, posEnd);
        String wholeNumericPattern = filterPattern.substring(pos + 2, posEnd + 1);
        if (from == -1 && to == -1) {
            filterPattern = filterPattern.replace("[[" + wholeNumericPattern + "]]", "");
        } else {
            boolean hasFrom = false;
            String newNumericPattern = "";
            if (from != -1) {
                newNumericPattern = numericPattern + ">=" + from;
                hasFrom = true;
            }
            if (to != -1) {
                if (hasFrom) {
                    newNumericPattern += " AND ";
                }
                newNumericPattern += numericPattern + "<=" + to;

            }
            String newWholeNumericPattern = wholeNumericPattern.replace(numericPattern, newNumericPattern);
            filterPattern = filterPattern.replace("[[" + wholeNumericPattern + "]]", newWholeNumericPattern);

        }
        return filterPattern;

    }

    protected static String getLiteralFilter(String filterPattern, String filterExpr) {
        System.out.println("===>filterPattern: " + filterPattern);
        int posFilter = 0;
        int posEndFilter = 0;
        String literalPatternFilter = "";
        String wholeLiteralPatternFilter = "";
        if (filterPattern.contains("[[FILTER(")) {
            System.out.println("===>FILTER<=====");
            posFilter = filterPattern.indexOf("[[FILTER(");
            posEndFilter = filterPattern.indexOf(")]]");
            literalPatternFilter = filterPattern.substring(posFilter + 9, posEndFilter);
            wholeLiteralPatternFilter = filterPattern.substring(posFilter + 2, posEndFilter + 1);
        }

        int posValues = filterPattern.indexOf("[[VALUES(");
        int posEndValues = filterPattern.indexOf(")]]", posValues);
        String valueslPattern = filterPattern.substring(posValues + 9, posEndValues);
        String wholeValuesPattern = filterPattern.substring(posValues + 2, posEndValues + 1);
        System.out.println(" valueslPattern: " + valueslPattern);
        System.out.println(" wholeValuesPattern: " + wholeValuesPattern);

        int posRegex = 0;
        int posEndRegex = 0;
        String literalPatternRegex = "";
        String wholeLiteralPatternRegex = "";
        if (filterPattern.contains("[[REGEX(")) {
            System.out.println("===>REGEX<=====");
            posRegex = filterPattern.indexOf("[[REGEX(");
            posEndRegex = filterPattern.indexOf(")]]");
            literalPatternRegex = filterPattern.substring(posRegex + 8, posEndRegex);
            wholeLiteralPatternRegex = filterPattern.substring(posRegex + 2, posEndRegex + 1);
        }

        if (filterExpr.startsWith("##")) {
            String valuesExpr = "FILTER( " + valueslPattern + " in " + filterExpr + ")";
            filterPattern = filterPattern.replace("[[" + wholeValuesPattern + "]]", valuesExpr.replaceAll("##", ""));
            filterPattern = filterPattern.replace("[[" + wholeLiteralPatternFilter + "]]", "");
            filterPattern = filterPattern.replace("[[" + wholeLiteralPatternRegex + "]]", "");
        } else if (filterExpr.equals("")) { //PAVLOS: In comment: || filterExpr.length() <= 8
            filterPattern = filterPattern.replace("[[" + wholeLiteralPatternFilter + "]]", "");
            filterPattern = filterPattern.replace("[[" + wholeValuesPattern + "]]", "");
            filterPattern = filterPattern.replace("[[" + wholeLiteralPatternRegex + "]]", "");
        } else if (!literalPatternFilter.equals("")) {
            String newLiteralPattern = "";
            newLiteralPattern = " . " + literalPatternFilter + " bif:contains " + filterExpr;

            String newWholeDatePattern = wholeLiteralPatternFilter.replace(literalPatternFilter, newLiteralPattern);
            newWholeDatePattern = newWholeDatePattern.replace("FILTER(", "");
            newWholeDatePattern = newWholeDatePattern.replace(")", "");
            filterPattern = filterPattern.replace("[[" + wholeLiteralPatternFilter + "]]", newWholeDatePattern);
            filterPattern = filterPattern.replace("[[" + wholeValuesPattern + "]]", "");
        } else if (!literalPatternRegex.contentEquals("")) {
            String newLiteralPattern = ".\n";
            filterExpr = filterExpr.replaceAll("'", "");
            filterExpr = filterExpr.replaceAll("\"", "");

            newLiteralPattern += "FILTER (";
            String parts[] = filterExpr.split("OR");
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i];
                newLiteralPattern += "REGEX(" + literalPatternRegex + ",\"" + p + "\", \"i\")";
                if (i < parts.length - 1) {
                    newLiteralPattern += " OR ";
                }
            }
            newLiteralPattern += ")";

            filterPattern = filterPattern.replace("[[" + wholeLiteralPatternRegex + "]]", newLiteralPattern);
            filterPattern = filterPattern.replace("[[" + wholeValuesPattern + "]]", "");

        }
        return filterPattern;

    }

    protected static String getGeoFilter(String filterPattern, String north, String south, String east, String west) {
        int pos = filterPattern.indexOf("[[GEOFILTER(");
        int posEnd = filterPattern.indexOf(")]]");

        String pattern = filterPattern.substring(pos + 12, posEnd);
        String wholeLPattern = filterPattern.substring(pos + 2, posEnd + 1);

        if (north.equals("") && south.equals("") && east.equals("") && west.equals("")) {
            filterPattern = filterPattern.replace("[[" + wholeLPattern + "]]", "");
        } else {
            String[] patternParts = pattern.split(";");
            String expr = "FILTER(";
            if (patternParts.length == 4) {
                expr += patternParts[0] + "<=" + east + " && " + patternParts[1] + ">=" + west + " && "
                        + patternParts[2] + "<=" + north + " && " + patternParts[3] + ">=" + south;
            } else if (patternParts.length == 2) {
                expr += patternParts[0] + "<=" + east + " && " + patternParts[0] + ">=" + west + " && "
                        + patternParts[1] + "<=" + north + " && " + patternParts[1] + ">=" + south;

            }
            expr += ")";

            filterPattern = filterPattern.replace("[[" + wholeLPattern + "]]", expr);
        }
        return filterPattern;

    }

    private String getFilterExpression(ArrayList<String> keywords) {
        StringBuilder sourceFilterExpression = new StringBuilder();
        if (!keywords.isEmpty()) {
            sourceFilterExpression.append("\"'");
            sourceFilterExpression.append(keywords.get(0)).append("'"); //removed * |  TO COMMIT!
            if (keywords.size() > 1) {
                for (int i = 1; i < keywords.size(); i++) {
                    sourceFilterExpression.append(" OR '").append(keywords.get(i)).append("'"); //removed * | TO COMMIT!
                }
            }
            sourceFilterExpression.append("\"");
            // System.out.println(" >> Source filter expression: " +
            // sourceFilterExpression.toString());
        }
        return sourceFilterExpression.toString();
    }

    private String getMoreGraphPatterns(JSONArray array, int counter) {
        ArrayList<String> firstMoreGraphPatters = new ArrayList<>();
        HashMap<Integer, String> pattern2expression = new HashMap<>();
        HashMap<Integer, String> pattern2morepatterns = new HashMap<>();
        int newindex = counter * 10;

        int rowModelSize = array.size();
        System.out.println(">> We have " + rowModelSize + " 'more' models...");

        for (int kk = 0; kk < rowModelSize; kk++) { // rowModelSize
            System.out.println(" >> Working on the model number '" + kk + "'...");
            JSONObject moreModel = (JSONObject) array.get(kk);
            String filterExpression = (String) moreModel.get("outerSelectedFilterExpression");
            if (filterExpression == null) {
                filterExpression = "";
            }
            // TO DO: handle OR expressions!
            String mgp = getObjectModelPattern(moreModel, counter, ++newindex);
            firstMoreGraphPatters.add(mgp);
            pattern2expression.put(kk, filterExpression);

            // Check for additional path relations...
            JSONArray newArray = (JSONArray) moreModel.get("rowModelList");
            int morerowModelSize = newArray.size();
            System.out.println("    >> more row model size = " + morerowModelSize);

            StringBuilder moreFilterPattern = new StringBuilder();
            for (int ll = 0; ll < morerowModelSize; ll++) {
                System.out.println("      >> working on the more row model " + ll + "...");
                int index2 = newindex + ll + 1;
                JSONObject moremoreModel = (JSONObject) newArray.get(ll);
                String moreMgp = getObjectModelPattern(moremoreModel, newindex, index2);
                String moreFilterExpression = (String) moremoreModel.get("outerSelectedFilterExpression");
                if (moreFilterExpression == null) {
                    moreFilterExpression = "";
                }
                System.out.println("    >> filter expression: " + filterExpression);
                System.out.println("    >> counter = " + counter + ", newindex = " + newindex + "");

                String nextexpression = "";
                if (ll != morerowModelSize - 1) {
                    JSONObject temp = (JSONObject) newArray.get(ll + 1);
                    nextexpression = (String) temp.get("outerSelectedFilterExpression");
                }

                if (nextexpression.toLowerCase().equals("or")) {
                    moreFilterPattern.append(" { ").append(moreMgp).append(" } UNION ");
                } else {
                    if (moreFilterExpression.toLowerCase().equals("or")) {
                        moreFilterPattern.append(" { ").append(moreMgp).append(" } ");
                    } else {
                        moreFilterPattern.append(moreMgp).append(" . ");
                    }
                }
            }

            pattern2morepatterns.put(kk, moreFilterPattern.toString());

            newindex += kk;
            newindex += morerowModelSize;
        }

        StringBuilder firstMoreGPs = new StringBuilder();
        System.out.println("   >> creating the full 'more' expression...");
        for (int pp = 0; pp < firstMoreGraphPatters.size(); pp++) {
            String firstMoreGP = firstMoreGraphPatters.get(pp);
            String expression = pattern2expression.get(pp);
            String morePattern = pattern2morepatterns.get(pp);

            String nextexpression = "";
            if (pp != firstMoreGraphPatters.size() - 1) {
                nextexpression = pattern2expression.get(pp + 1);
            }
            if (expression == null) {
                expression = "";
            }
            if (nextexpression == null) {
                nextexpression = "";
            }

            if (nextexpression.toLowerCase().equals("or")) {
                firstMoreGPs.append(" { ");
                firstMoreGPs.append(firstMoreGP);
                if (morePattern != null) {
                    if (!firstMoreGP.trim().endsWith(".")) {
                        firstMoreGPs.append(" . ");
                    }
                    firstMoreGPs.append(morePattern);
                }
                firstMoreGPs.append(" } UNION ");
            } else {
                if (expression.toLowerCase().equals("or")) {
                    firstMoreGPs.append(" { ");
                    firstMoreGPs.append(firstMoreGP);
                    if (morePattern != null) {
                        if (!firstMoreGP.trim().endsWith(".")) {
                            firstMoreGPs.append(" . ");
                        }
                        firstMoreGPs.append(morePattern);
                    }
                    firstMoreGPs.append(" } ");
                } else {
                    firstMoreGPs.append(firstMoreGP);
                    if (morePattern != null) {
                        if (!firstMoreGP.trim().endsWith(".")) {
                            firstMoreGPs.append(" . ");
                        }
                        firstMoreGPs.append(morePattern);
                    }
                    firstMoreGPs.append(" . ");
                }
            }
        }
        return firstMoreGPs.toString();
    }

    public String getObjectModelPattern(Object object, int counter, int newindex) {

        JSONObject moreModel = (JSONObject) object;// take the first model; FUTURE WORK: take all MODELS!!!
        System.out.println("      more model: " + moreModel);

        JSONObject selRelat = (JSONObject) moreModel.get("selectedRelation");
        JSONObject moreSelectedRelatedEntity = (JSONObject) moreModel.get("selectedRelatedEntity");

        String moreRelationGraphPattern = (String) selRelat.get("relation_graph_pattern");
        String moreDestinationFilterPattern = (String) selRelat.get("destination_filter_pattern");
        System.out.println("     selected relation: " + selRelat.get("name"));

        ArrayList<String> keywords = getKeywords((JSONArray) moreModel.get("relatedChips"));

        String moreSelectedRelatedEntityTypeS = (String) moreSelectedRelatedEntity.get("entityType");
        System.out.println("     moreSelectedRelatedEntityTypeS:::: " + moreSelectedRelatedEntityTypeS);
        EntityType moreSelectedRelatedEntityType = EntityTypes.getEntityType(moreSelectedRelatedEntityTypeS);
        String filterExpression = "";
        JSONObject moreRangeOfDates = (JSONObject) moreModel.get("rangeOfDates");
        String moreDateFrom = "";
        String moreDateUntil = "";
        if (moreRangeOfDates != null) {
            moreDateFrom = (String) moreRangeOfDates.get("from");
            moreDateUntil = (String) moreRangeOfDates.get("until");
        }

        JSONObject moreRangeOfTimePrimitive = (JSONObject) moreModel.get("timePrimitive");
        String moreTimePrimitiveFrom = "";
        String moreTimePrimitiveTo = "";
        if (moreRangeOfTimePrimitive != null) {
            moreTimePrimitiveFrom = (String) moreRangeOfTimePrimitive.get("from");
            moreTimePrimitiveTo = (String) moreRangeOfTimePrimitive.get("to");
        }

        JSONObject moreBoundingBox = (JSONObject) moreModel.get("boundingBox");

        JSONArray moretSelectedRelatedInstanceList = (JSONArray) moreModel.get("selectedRelatedInstanceList");
        JSONObject numericRange = (JSONObject) moreModel.get("numericRange");
        double numericFrom = -1;
        double numericTo = -1;
        if (numericRange != null) {
            if (numericRange.get("from") != null) {
                numericFrom = Double.parseDouble(numericRange.get("from").toString());
            }
            if (numericRange.get("to") != null) {
                numericTo = Double.parseDouble(numericRange.get("to").toString());
            }
        }

        Boolean booleanValues = (Boolean) moreModel.get("booleanValues");
        boolean flag = false;

        if ((!keywords.isEmpty() || moretSelectedRelatedInstanceList.size() > 0) && moreBoundingBox == null) {
            filterExpression = getFilterExpression(keywords);
            if (moretSelectedRelatedInstanceList.size() > 0) {
                filterExpression = "##" + getValues(moretSelectedRelatedInstanceList) + "##";
            }
            flag = true;
        } else if (!moreDateFrom.equals("") || !moreDateUntil.equals("")) {
            filterExpression = moreDateFrom + "##" + moreDateUntil;
            flag = true;
        } else if (!moreTimePrimitiveFrom.equals("") || !moreTimePrimitiveTo.equals("")) {
            filterExpression = moreTimePrimitiveFrom + "##" + moreTimePrimitiveTo;
            flag = true;
        } else if (moreBoundingBox != null) {
            String north = String.valueOf(moreBoundingBox.get("north"));
            String south = String.valueOf(moreBoundingBox.get("south"));
            String west = String.valueOf(moreBoundingBox.get("west"));
            String east = String.valueOf(moreBoundingBox.get("east"));

            filterExpression = "--" + north + "##" + south + "##" + east + "##" + west + "--";

            if (!keywords.isEmpty()) {

                if (moretSelectedRelatedInstanceList.size() > 0) {
                    filterExpression += "##" + getValues(moretSelectedRelatedInstanceList) + "##";
                } else {
                    filterExpression += getFilterExpression(keywords);
                }
            }
            flag = true;
        } else if (numericFrom != -1 || numericTo != -1) {
            filterExpression = String.valueOf(numericFrom) + "##" + String.valueOf(numericTo);
            flag = true;
        } else if (booleanValues != null) {
            filterExpression = booleanValues.toString();
            flag = true;
        }

        if (flag) {
            if (moreRelationGraphPattern.trim().endsWith(".")) {
                moreRelationGraphPattern = moreRelationGraphPattern + moreDestinationFilterPattern;
            } else {
                moreRelationGraphPattern = moreRelationGraphPattern + " . " + moreDestinationFilterPattern;
            }
        }
        moreRelationGraphPattern = getFilterPattern(moreSelectedRelatedEntityType, moreRelationGraphPattern,
                filterExpression, counter, newindex);

        return moreRelationGraphPattern;
    }

    @RequestMapping(value = "/retrieve_entity_info", method = RequestMethod.POST, produces = {
        "application/json;charset=utf-8"})
    public @ResponseBody
    JSONObject retrieveEntityInfo(
            @RequestHeader(value = "Authorization") String authorizationToken, @RequestBody JSONObject requestParams)
            throws IOException, ParseException {
        System.out.println("retrieveEntityInfo() in QueryController.java");
        System.out.println(" >> allParams: " + requestParams.keySet());
        System.out.println("  param entityUri: " + requestParams.get("entityUri"));
        System.out.println("  param fromSearch: " + requestParams.get("fromSearch"));
        System.out.println("  param entityName: " + requestParams.get("entityName"));

        HashMap<Integer, EntityModel> configEntities = PropertiesService.getEntities();
        String exploreQuery = "";
        String exploreVariableTypes = "";
        for (int entid : configEntities.keySet()) {
            EntityModel entmodel = configEntities.get(entid);
            if (entmodel.getName().equals(requestParams.get("entityName").toString())) {
                exploreQuery = entmodel.getExploreQuery();
                exploreVariableTypes = entmodel.getExploreVariableTypes();
                break;
            }
        }
        // Handling request parameters
        String entityUriStr = null;
        String fromSearchStr = null;

        if (requestParams.get("entityUri") != null) {
            entityUriStr = requestParams.get("entityUri").toString();
        }
        if (requestParams.get("fromSearch") != null) {
            fromSearchStr = requestParams.get("fromSearch").toString();
        }

        exploreQuery = exploreQuery.replace("[[FROM_GRAPHS]]", fromSearchStr);
        exploreQuery = exploreQuery.replace("[[URI]]", "<" + entityUriStr + ">");
        restClient = new VirtuosoRestClient(serviceUrl, authorizationToken);
        Response serviceResponce = restClient.executeSparqlQuery(exploreQuery, 0, "application/json",
                authorizationToken);
        String title = "";
        JSONObject responseJsonObject = new JSONObject(); // JSON Object to hold response

        if (serviceResponce.getStatus() == 200) {
            LinkedHashMap<String, String> varToEntityNameMap = getExploreVariablesMap(exploreVariableTypes);
            if (requestParams.get("title").getClass() != String.class) {
                LinkedHashMap titleMap = (LinkedHashMap) requestParams.get("title");

                String entityType = (String) requestParams.get("entityName");
                String entityuri = (String) requestParams.get("entityUri");

                String nameToShow = entityuri;
                for (Object values : titleMap.values()) {
                    boolean found = false;
                    for (Object ob : ((ArrayList) values)) {
                        String uri = (String) ((HashMap) ob).get("uri");
                        String value = (String) ((HashMap) ob).get("value");
                        if (uri.equals(entityuri)) {
                            nameToShow = value;
                            found = true;
                            break;
                        }
                    }
                    if (found) {
                        break;
                    }
                }
                title = nameToShow + " (" + entityType + ")";
                if (title.length() > 50) {
                    title = title.substring(0, 49) + "...";
                }
            } else {
                String entityname = (String) requestParams.get("entityName");
                title = (String) requestParams.get("title");
                if (!title.endsWith("(" + entityname + ")")) {
                    title += " (" + entityname + ")";
                }
                if (title.length() > 50) {
                    title = title.substring(0, 49) + "...";
                }
            }
            JSONObject qFinalRsults = (JSONObject) serviceResponce.readEntity(JSONObject.class);

            ArrayList bindings = ((ArrayList) ((LinkedHashMap) qFinalRsults.get("results")).get("bindings"));
            LinkedHashMap varToValues = getExploreResultMap(bindings, varToEntityNameMap,
                    requestParams.get("entityName").toString());
            responseJsonObject.put("result", varToValues);

        }
        beautifyQueryResultsService = new BeautifyQueryResultsService(authorizationToken, serviceUrl);
        //beautifyQueryResultsService.enrichAllInAndOutProperties(entityUriStr, fromSearchStr);

        // Getting JSON output and place it into response JSON Object
        // responseJsonObject = beautifyQueryResultsService.getInstanceInfo();
        responseJsonObject.put("title", title);

        System.out.println("responseJsonObject ::  " + responseJsonObject);
        return responseJsonObject;
    }

    private LinkedHashMap getExploreResultMap(ArrayList bindings, LinkedHashMap varToEntityNameMap,
            String currEntityName) {
        LinkedHashMap varToValuesTmp = new LinkedHashMap<String, ArrayList>();
        LinkedHashMap varToValues = new LinkedHashMap<String, ArrayList>();

        for (int i = 0; i < bindings.size(); i++) {
            LinkedHashMap binding = (LinkedHashMap) bindings.get(i);
            Iterator variables = binding.keySet().iterator();
            while (variables.hasNext()) {
                String variable = (String) variables.next();
                String[] variableParts = variable.split("__");
                String uriValue = "";
                String entityName = currEntityName;

                if (variableParts.length == 2 && varToEntityNameMap.containsKey(variableParts[0])) {
                    String uriVar = variableParts[0];
                    entityName = (String) varToEntityNameMap.get(uriVar);
                    uriValue = (String) (String) ((LinkedHashMap) binding.get(uriVar)).get("value");
                }

                String value = (String) ((LinkedHashMap) binding.get(variable)).get("value");
                String type = (String) ((LinkedHashMap) binding.get(variable)).get("type");

                if (!varToEntityNameMap.containsKey(variable)) {
                    if (!varToValues.containsKey(variable)) {
                        ArrayList values = new <String>ArrayList();
                        ArrayList uris = new <String>ArrayList();
                        ArrayList entityNames = new <String>ArrayList();
                        ArrayList types = new <String>ArrayList();
                        values.add(value);
                        uris.add(uriValue);
                        entityNames.add(entityName);
                        types.add(type);
                        ArrayList all = new ArrayList();
                        all.add(values);
                        all.add(uris);
                        all.add(entityNames);
                        all.add(types);
                        varToValuesTmp.put(variable, all);
                    } else {
                        ArrayList all = (ArrayList) varToValuesTmp.get(variable);
                        ArrayList values = (ArrayList) all.get(0);
                        ArrayList uris = (ArrayList) all.get(1);
                        ArrayList entityNames = (ArrayList) all.get(2);
                        ArrayList types = (ArrayList) all.get(3);

                        if (!values.contains(value)) {
                            values = (ArrayList) all.get(0);
                            values.add(value);
                            uris.add(uriValue);
                            entityNames.add(entityName);
                            types.add(type);
                            varToValuesTmp.put(variable, all);
                        }
                    }
                }

            }
            Iterator it = varToValuesTmp.keySet().iterator();
            while (it.hasNext()) {
                String variable = (String) it.next();
                ArrayList values = (ArrayList) ((ArrayList) varToValuesTmp.get(variable)).get(0);
                ArrayList uris = (ArrayList) ((ArrayList) varToValuesTmp.get(variable)).get(1);
                ArrayList entityNames = (ArrayList) ((ArrayList) varToValuesTmp.get(variable)).get(2);
                ArrayList types = (ArrayList) ((ArrayList) varToValuesTmp.get(variable)).get(3);

                ArrayList all = new ArrayList();

                for (int j = 0; j < values.size(); j++) {
                    LinkedHashMap tmp = new LinkedHashMap<String, String>();
                    tmp.put("value", values.get(j));
                    tmp.put("uri", uris.get(j));
                    tmp.put("entityName", entityNames.get(j));
                    tmp.put("type", types.get(j));

                    all.add(tmp);

                }
                if (variable.contains("__")) {
                    String varData[] = variable.split("__");
                    if (varData.length == 2) {
                        variable = varData[1];
                    }
                }
                variable = variable.replaceAll("_", " ");

                if (varToValues.containsKey(variable)) {
                    ArrayList<Object> temp = (ArrayList<Object>) varToValues.get(variable);
                    for (Object o : all) {
                        if (!temp.contains(o)) {
                            temp.add(o);
                        }
                    }
                    varToValues.put(variable, temp);
                } else {
                    varToValues.put(variable, all);
                }

            }

        }

        return varToValues;

    }

    private LinkedHashMap getExploreVariablesMap(String exploreVariableTypes) throws IOException {
        LinkedHashMap<String, String> varToEntityNameMap = new LinkedHashMap();
        String[] exploreVariableTypesArr = exploreVariableTypes.split(";");
        HashMap<Integer, EntityModel> configEntities = PropertiesService.getEntities();

        for (String exploreVariableType : exploreVariableTypesArr) {
            String[] parts = exploreVariableType.split(":");
            if (parts.length == 2) {
                String var = parts[0].trim();
                int entity_id = Integer.parseInt(parts[1].trim());
                String entity_name = "";

                for (int entid : configEntities.keySet()) {
                    EntityModel entmodel = configEntities.get(entid);
                    if (entid == entity_id) {
                        entity_name = entmodel.getName();
                        break;
                    }
                }
                varToEntityNameMap.put(var, entity_name);
            }
        }

        return varToEntityNameMap;
    }

    private String getFilterPattern(EntityType type, String destinationFilterPattern, String filterExpression,
            int count, int index) {

        System.out.println(filterExpression + ":: getFilterPattern-filterExpression");
        System.out.println(type + ":: getFilterPattern-type");
        if (!filterExpression.contentEquals("")) {

            if (EntityType.LITERAL == type || EntityType.STRING == type) {
                destinationFilterPattern = getLiteralFilter(destinationFilterPattern, filterExpression);
            } else if (EntityType.TEMPORAL == type) {
                if (!filterExpression.equals("")) {
                    String dateFrom = filterExpression.split("##")[0];
                    String dateUntil = filterExpression.split("##")[1];
                    destinationFilterPattern = getDateFilter(destinationFilterPattern, dateFrom, dateUntil);

                }
            } else if (EntityType.TIME_PRIMITIVE == type) {
                if (!filterExpression.equals("")) {
                    String timePrimitiveFrom = "";
                    if (filterExpression.split("##").length > 0) {
                        timePrimitiveFrom = filterExpression.split("##")[0];
                    }
                    String timePrimitiveTo = "";
                    if (filterExpression.split("##").length > 1) {
                        timePrimitiveTo = filterExpression.split("##")[1];
                    }
                    destinationFilterPattern = getTimePrimitiveFilter(destinationFilterPattern, timePrimitiveFrom, timePrimitiveTo);

                }
            } else if (EntityType.LITERAL_GEOSPATIAL == type) {
                if (filterExpression.startsWith("--")) { // it has bounding box

                    String[] parts = filterExpression.split("--");
                    String coordinates = parts[1];
                    String[] coordinatesParts = coordinates.split("##");

                    String keyword = "";
                    if (parts.length == 3) {
                        keyword = parts[2];
                    }
                    destinationFilterPattern = getLiteralFilter(destinationFilterPattern, keyword);
                    destinationFilterPattern = getGeoFilter(destinationFilterPattern, coordinatesParts[0],
                            coordinatesParts[1], coordinatesParts[2], coordinatesParts[3]);
                } else { // it has either keyword or instances
                    destinationFilterPattern = getLiteralFilter(destinationFilterPattern, filterExpression);
                    destinationFilterPattern = getGeoFilter(destinationFilterPattern, "", "", "", "");

                }

            } else if (EntityType.NUMERIC == type) {
                if (!filterExpression.equals("")) {
                    String from = filterExpression.split("##")[0];
                    String to = filterExpression.split("##")[1];
                    System.out.println("from : " + from + "to : " + to);
                    destinationFilterPattern = getNumericFilter(destinationFilterPattern, Double.valueOf(from),
                            Double.valueOf(to));

                }
            } else if (EntityType.BOOLEAN == type) {
                int pos = destinationFilterPattern.indexOf("[[FILTER(");
                int posEnd = destinationFilterPattern.indexOf(")]]");
                String booleanPattern = destinationFilterPattern.substring(pos + 9, posEnd);
                destinationFilterPattern = "FILTER(" + booleanPattern + " = " + Boolean.valueOf(filterExpression) + ")";

            }
        }
        destinationFilterPattern = destinationFilterPattern.replace("[[i]]", "_" + count);
        destinationFilterPattern = destinationFilterPattern.replace("[[j]]", "_" + index);
        destinationFilterPattern = destinationFilterPattern.replace("[[k]]", "_" + index);
        System.out.println("destinationFilterPattern " + destinationFilterPattern);
        return destinationFilterPattern;
    }

}
