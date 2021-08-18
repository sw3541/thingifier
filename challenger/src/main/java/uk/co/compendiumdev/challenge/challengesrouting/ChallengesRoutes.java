package uk.co.compendiumdev.challenge.challengesrouting;

import uk.co.compendiumdev.challenge.ChallengerAuthData;
import uk.co.compendiumdev.challenge.apimodel.ChallengeThingifier;
import uk.co.compendiumdev.challenge.challengers.Challengers;
import uk.co.compendiumdev.challenge.challenges.ChallengeDefinitions;
import uk.co.compendiumdev.thingifier.api.ThingifierApiDefn;
import uk.co.compendiumdev.thingifier.api.response.ApiResponse;
import uk.co.compendiumdev.thingifier.api.restapihandlers.RestApiGetHandler;
import uk.co.compendiumdev.thingifier.api.routings.RoutingDefinition;
import uk.co.compendiumdev.thingifier.api.routings.RoutingStatus;
import uk.co.compendiumdev.thingifier.api.routings.RoutingVerb;
import uk.co.compendiumdev.thingifier.application.AdhocDocumentedSparkRouteConfig;
import uk.co.compendiumdev.thingifier.application.routehandlers.SparkApiRequestResponseHandler;
import uk.co.compendiumdev.thingifier.core.domain.definitions.EntityDefinition;
import uk.co.compendiumdev.thingifier.spark.SimpleRouteConfig;

import java.util.ArrayList;
import java.util.Map;

import static spark.Spark.*;

public class ChallengesRoutes {
    
    public void configure(final Challengers challengers, final boolean single_player_mode,
                          final ThingifierApiDefn apiDefn,
                          final ChallengeDefinitions challengeDefinitions){

        get("/challenges", (request, result) -> {

            ChallengerAuthData challenger = challengers.getChallenger(request.headers("X-CHALLENGER"));

            if(!single_player_mode){
                if(challenger!=null){
                    result.raw().setHeader("Location", "/gui/challenges/" + challenger.getXChallenger());
                }
            }else{
                result.raw().setHeader("Location", "/gui/challenges");
            }

            ChallengeThingifier challengeThingifier = new ChallengeThingifier();
            final EntityDefinition challengeDefn = challengeThingifier.challengeDefn;
            challengeThingifier.populateThingifierFrom(challengeDefinitions);

            return new SparkApiRequestResponseHandler(request, result, challengeThingifier.challengeThingifier).
                    usingHandler((anHttpApiRequest)->{
                        challengeThingifier.populateThingifierFromStatus(challenger);
                        final Map<String, String> queryParams = anHttpApiRequest.getQueryParams();
                        if(!queryParams.containsKey("sortBy") &&
                                !queryParams.containsKey("sort_by")){
                            // force a sort
                            queryParams.put("sort_by","+ID");
                        }
                        return new RestApiGetHandler(challengeThingifier.challengeThingifier)
                                .handle(challengeDefn.getPlural(),
                                        queryParams);
                    }).handle();

        });

        apiDefn.addRouteToDocumentation(
                new RoutingDefinition(
                        RoutingVerb.GET,
                        "/challenges",
                        RoutingStatus.returnedFromCall(),
                        null).addDocumentation("Get list of challenges and their completion status").
                        addPossibleStatuses(200));

        // TODO: because these hardcode contentType and ignore Accept there should be a light weight wrapper available
        new AdhocDocumentedSparkRouteConfig(apiDefn).
                add("/challenges", RoutingVerb.HEAD, 200, "Headers for list of challenges endpoint",
                        (request, result) ->{
                            result.status(200);
                            result.type("application/json");
                            return "";
                        }).
                add("/challenges", RoutingVerb.OPTIONS, 200,"Options for list of challenges endpoint",
                        ((request, result) -> {
                            result.status(200);
                            result.type("application/json");
                            result.header("Allow", "GET, HEAD, OPTIONS");
                            return "";
                        }));

        SimpleRouteConfig.routeStatusWhenNot(
                405, "/challenges",
                "get", "head", "options");

    }

}
