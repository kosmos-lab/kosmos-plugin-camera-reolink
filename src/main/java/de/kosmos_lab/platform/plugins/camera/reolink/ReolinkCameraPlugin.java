package de.kosmos_lab.platform.plugins.camera.reolink;



import de.kosmos_lab.platform.plugins.camera.ICamera;
import de.kosmos_lab.platform.plugins.camera.exceptions.VideoNotAvailableException;
import de.kosmos_lab.utils.FFMPEGWrapper;
import de.kosmos_lab.utils.KosmosFileUtils;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.InputStreamResponseListener;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.pf4j.Extension;
import org.pf4j.Plugin;
import org.pf4j.PluginWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ReolinkCameraPlugin extends Plugin {
    protected static final Logger logger = LoggerFactory.getLogger("ReolinkCameraPlugin");
    
    public ReolinkCameraPlugin(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    public void start() {
        System.out.println("ReolinkCameraPlugin.start(2)");
    }
    
    @Override
    public void stop() {
        System.out.println("ReolinkCameraPlugin.stop()");
    }
   
    
}

