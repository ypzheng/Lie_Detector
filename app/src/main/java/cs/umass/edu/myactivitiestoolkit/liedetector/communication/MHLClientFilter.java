package cs.umass.edu.myactivitiestoolkit.liedetector.communication;

/**
 * Identifies common data filters used when receiving data from the server.
 * <br><br>
 * As an example, to listen for steps and activities detected, register a
 * message receiver object as follows:
 *
 * <pre>
 * {@code setMessageReceiver(new MessageReceiver(MessageReceiver.Filter.STEP_DETECTED, MessageReceiver.Filter.ACTIVITY_DETECTED) {
 *         @literal @Override
 *          void onMessageReceived(JSONObject json) {
 *              //parse json, handle message
 *          }
 *   });
 * }
 * </pre>
 *
 * @author Sean Noran
 */
public interface MHLClientFilter {
    String AVERAGE_ACCELERATION = "AVERAGE_ACCELERATION";
    String STEP = "STEP";
    String ACTIVITY = "ACTIVITY";
    String SPEAKER = "SPEAKER";
}