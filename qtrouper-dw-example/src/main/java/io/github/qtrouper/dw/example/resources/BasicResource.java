package io.github.qtrouper.dw.example.resources;

import io.github.qtrouper.core.models.QueueContext;
import io.github.qtrouper.dw.example.troupers.Acknowledger;
import io.github.qtrouper.dw.example.troupers.NotificationSender;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/basic")
@Produces(MediaType.APPLICATION_JSON)
public class BasicResource {

  private final NotificationSender notificationSender;
  private final Acknowledger acknowledger;

  @Inject
  public BasicResource(NotificationSender notificationSender, Acknowledger acknowledger) {
    this.notificationSender = notificationSender;
    this.acknowledger = acknowledger;
  }

  @Path("/ping")
  @GET
  public Response ping() {
    return Response.ok("pong").build();
  }


  @Path("/notify")
  @GET
  public Response sendNotification(@QueryParam("notificationId") String notificationId)
      throws Exception {
    notificationSender.publish(QueueContext.builder()
        .serviceReference(notificationId)
        .build());
    return Response.ok("Published to notification queue").build();
  }

  @Path("/ack")
  @GET
  public Response ack(@QueryParam("ackId") String ackId)
      throws Exception {
    acknowledger.publish(QueueContext.builder()
        .serviceReference(ackId)
        .build());
    return Response.ok("Published to acknowledger queue").build();
  }

}
