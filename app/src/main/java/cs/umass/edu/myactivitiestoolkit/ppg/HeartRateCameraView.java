package cs.umass.edu.myactivitiestoolkit.ppg;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cs.umass.edu.myactivitiestoolkit.util.ImageFormatConverter;

/**
 *  The heart rate camera view acquires PPG data from using the phone's camera. It sets up
 *  a {@link Camera} object with the appropriate parameters, e.g. the preview size and resolution
 *  and flash mode, and registers a {@link android.hardware.Camera.PreviewCallback} for receiving
 *  pixel data each frame. The {@link #onPreviewFrame(byte[], Camera)} method is called with each
 *  frame. The raw data is in NV21 format and is converted already to RGB format for you.
 *  <br><br>
 *  <b>ASSIGNMENT 4 (PHOTOPLETHYSMOGRAPHY)</b> :
 *  In {@link #onPreviewFrame(byte[], Camera)}, you should extract the mean red value from the
 *  pixel data and send it to all {@link PPGListener}s, maintained in the {@link #listeners}
 *  list. To ensure clean modular design, all PPG processing and heart-beat detection should
 *  be done in the {@link PPGListener#onSensorChanged(PPGEvent)} event, which you override
 *  in the {@link cs.umass.edu.myactivitiestoolkit.services.PPGService}. Do NOT call your
 *  heart rate detection algorithm from within this class.
 *  <br><br>
 *  EXTRA CREDIT:
 *      The {@link Camera} class is deprecated after API level 21 (Lollipop) and may not
 *      be fully supported in future APIs. Instead, you should use the
 *      {@link android.hardware.camera2} API to fully support newer hardware. For extra
 *      credit, re-implement the Photoplethysmography sensor using the camera2 API.
 *
 * @see Camera
 * @see android.hardware.Camera.PreviewCallback#onPreviewFrame(byte[], Camera)
 * @see PPGEvent
 * @see PPGListener
 * @see SurfaceView
 */
@SuppressWarnings("deprecation") // the Camera class is deprecated, but we don't care for this assignment
public class HeartRateCameraView extends SurfaceView implements Callback, Camera.PreviewCallback {

    /**
     * Once the preview surface has been created, the {@link #onSurfaceCreated()} method is called.
     * This is to ensure that other application components do not call {@link #start()} until
     * the surface has been created.
     */
    public interface SurfaceCreatedCallback {
        /**
         * Called when the surface has been created and the {@link #mCamera} object has been initialized.
         */
        void onSurfaceCreated();
    }

    /**
     * Handles the event that the preview surface has been created.
     */
    private SurfaceCreatedCallback surfaceCreatedCallback;

    /**
     * Registers a callback to listen for the event that the surface is created. This is
     * important to ensure that no application component calls {@link #start()} on a
     * {@link HeartRateCameraView} instance that has not yet been completely initialized.
     * @param surfaceCreatedCallback the callback implementation.
     */
    public void setSurfaceCreatedCallback(SurfaceCreatedCallback surfaceCreatedCallback){
        this.surfaceCreatedCallback = surfaceCreatedCallback;
    }

    @SuppressWarnings("unused")
    /** used for debugging purposes */
    private static final String TAG = HeartRateCameraView.class.getName();

    /** Maintains the camera display surface. */
    private SurfaceHolder mHolder;

    /** Handle to the camera object */
    private Camera mCamera;

    /** Camera preview width. */
    private int width;

    /** Camera preview height. */
    private int height;

    /**
     * Indicates whether the camera preview should be displayed.
     * Setting this to true is useful for debugging.
     */
    private static final boolean showCamera = true;

    /** The number of pixels in the camera preview, i.e. {@link #width} * {@link #height}. */
    private int nPixels;

    /** The array of RGB pixels in the camera preview data */
    private int[] pixels;

    /**
     * The list of all clients listening for PPG sensor events.
     */
    private final ArrayList<PPGListener> listeners = new ArrayList<>();

    public HeartRateCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
    }

    /**
     * Registers a client to listen for PPG sensor events.
     * @param listener a listener implementation.
     */
    public void registerListener(PPGListener listener){
        listeners.add(listener);
    }

    /**
     * Unregisters the specified listener.
     * @param listener the reference to the listener to be unregistered.
     */
    public void unregisterListener(PPGListener listener){
        listeners.remove(listener);
    }

    /**
     * Unregisters all PPG sensor event listeners. No sensor events will be relayed to
     * any other application components as long as there are no registered listeners.
     */
    public void unregisterListeners(){
        listeners.clear();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // nothing to do
    }

    public void surfaceCreated(SurfaceHolder holder) {
        synchronized (this) {
            initializeCamera();
            if (showCamera) {
                try {
                    mCamera.setPreviewDisplay(getHolder());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (surfaceCreatedCallback != null)
            surfaceCreatedCallback.onSurfaceCreated();
    }

    /**
     * Starts the camera preview for collecting PPG data. This MUST be called after
     * {@link #surfaceCreated(SurfaceHolder)} has been called or {@link #mCamera} will be
     * null and a {@link NullPointerException} will be thrown. It is not guaranteed that
     * the surface will be created before calling this method; therefore, make sure to
     * register a {@link SurfaceCreatedCallback} to be notified.
     */
    public void start(){
        mCamera.startPreview(); // start the camera
        mCamera.setPreviewCallback(this);
    }

    /**
     * Initializes the handle to the camera object by requesting use of the hardware camera
     * and sets the camera parameters appropriately.
     */
    public void initializeCamera(){
        mCamera = Camera.open();

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);

        List<Camera.Size> previewSizes = parameters.getSupportedPreviewSizes();
        // Get the supported preview size closest to the requested dimensions:
        Camera.Size previewSize = previewSizes.get(previewSizes.size() - 1); // getOptimalPreviewSize(previewSizes, width, height);
        width = previewSize.width;
        height = previewSize.height;
        Log.d(TAG, "width: " + width + " , height: " + height);
        nPixels = width * height;
        pixels = new int[nPixels];
        setSize(width, height);
        parameters.setPreviewSize(width, height);

        mCamera.setParameters(parameters);

        int dataBufferSize=(int)(height * width * (ImageFormat.getBitsPerPixel(parameters.getPreviewFormat())/8.0));

        mCamera.addCallbackBuffer(new byte[dataBufferSize]);
        mCamera.setPreviewCallbackWithBuffer(this);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    /**
     * Called when each frame is captured by the camera. We already decode the data
     * to RGB pixels for you. Your job is to compute the mean red value of each frame
     * and send that data to each {@link PPGListener} in {@link #listeners}. You can
     * use the {@link Color#red(int)} method to extract the amount of red from an RGB
     * value.
     * <br><br>
     * For better results, try averaging only over a portion of the image about its
     * center. You might try a radial or rectangular region.
     *
     * @param data the data, by default in NV21 (YUV420SP) image format.
     * @param camera a handle to the camera object.
     * @see android.graphics.ImageFormat#NV21
     */
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        camera.addCallbackBuffer(data);

        //Collect color data and store them but now replacing each frame with new frame
        ImageFormatConverter.decodeYUV420SP(pixels, data, width, height);

        //TODO: Compute the mean red value and notify all listeners
        /** START : REMOVE FROM START CODE **/
        int redSum = 0; //, greenSum = 0, blueSum = 0;

        for (int rgb : pixels){
            redSum+=Color.red(rgb);
        }

        double meanRed = (redSum / nPixels);

        PPGEvent event = new PPGEvent(meanRed, System.currentTimeMillis());
        for (PPGListener listener : listeners){
            listener.onSensorChanged(event);
        }
        /** END : REMOVE FROM START CODE **/
    }

    /**
     * Sets the size of the camera preview surface holder. Note this does not
     * change the resolution of the preview.
     * @param width the width of the surface holder.
     * @param height the height of the surface holder.
     */
    public void setSize(int width, int height){
        mHolder.setFixedSize(width, height);
    }

    /**
     * Stops the camera recording and releases the camera for other applications to use.
     */
    public void stop(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        if (mHolder != null) {
            mHolder.getSurface().release();
            mHolder = null;
        }
    }
}