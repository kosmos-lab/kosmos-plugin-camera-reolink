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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Extension
public class ReolinkCamera implements ICamera {
    protected static final Logger logger = LoggerFactory.getLogger("ReolinkCamera");
    
    private final String password;
    private final String username;
    private final String base;
    private final HttpClient client;
    private final String name;
    private String mydir = "tmp";
    
    
    public ReolinkCamera(JSONObject options) {
        this.password = options.getString("password");
        this.username = options.getString("username");
        this.name = options.getString("name");
        this.base = options.getString("base") + "/cgi-bin/api.cgi?user=" + username + "&password=" + password;
        this.client = new HttpClient();
        try {
            this.client.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Calendar convertToCal(JSONObject json) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, json.getInt("year"));
        c.set(Calendar.DAY_OF_MONTH, json.getInt("day"));
        c.set(Calendar.MONTH, json.getInt("mon") - 1);
        c.set(Calendar.HOUR_OF_DAY, json.getInt("hour"));
        c.set(Calendar.MINUTE, json.getInt("min"));
        c.set(Calendar.SECOND, json.getInt("sec"));
        return c;
        
    }
    
    private JSONObject convertToJSON(Calendar cal) {
        JSONObject json = new JSONObject();
        json.put("year", cal.get(Calendar.YEAR));
        json.put("mon", cal.get(Calendar.MONTH) + 1);
        json.put("day", cal.get(Calendar.DAY_OF_MONTH));
        json.put("hour", cal.get(Calendar.HOUR_OF_DAY));
        json.put("min", cal.get(Calendar.MINUTE));
        json.put("sec", cal.get(Calendar.SECOND));
        return json;
    }
    
    private Request createRequest(HttpMethod method, HashMap<String, String> parameters, JSONObject json) {
        Request request = client.newRequest(base);
        if (parameters != null) {
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                request.param(e.getKey(), e.getValue());
            }
        }
        if (json != null) {
            //request.header("Content-Type","application/json");
            request.content(new StringContentProvider(json.toString()), "application/json");
        }
        request.method(method);
        request.agent("KosmoS Client");
        return request;
    }
    
    private Request createRequest(HttpMethod method, HashMap<String, String> parameters, JSONArray json) {
        return createRequest(null, method, parameters, json);
    }
    
    private Request createRequest(String url, HttpMethod method, HashMap<String, String> parameters, JSONArray json) {
        Request request;
        if (url != null) {
            request = client.newRequest(base + url);
        } else {
            request = client.newRequest(base);
        }
        
        if (parameters != null) {
            for (Map.Entry<String, String> e : parameters.entrySet()) {
                request.param(e.getKey(), e.getValue());
            }
        }
        if (json != null) {
            //request.header("Content-Type","application/json");
            request.content(new StringContentProvider(json.toString()), "application/json");
        }
        request.method(method);
        request.agent("KosmoS Client");
        return request;
    }
    
    private void download(String name) {
        
        String fname = "tmp/" + name;
        logger.info("downloading recording {} to {}", name, fname);
        File f = new File(fname);
        if (!f.exists()) {
            logger.info("file does not exist yet");
            Request req = this.createRequest("&cmd=Download&source=" + name + "&ouptut=" + name, HttpMethod.GET, null, (JSONArray) null);
            try {
                InputStreamResponseListener listener = new InputStreamResponseListener();
                
                req.send(listener);
                Response response = listener.get(20, TimeUnit.SECONDS);
                
                if (response.getStatus() == 200) {
                    // Obtain the input stream on the response content
                    try (InputStream input = listener.getInputStream()) {
                        byte[] bytes = input.readAllBytes();
                        
                        logger.info("read {} for file {}", bytes.length, fname);
                        KosmosFileUtils.writeToFile(f, bytes);
                        
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    logger.warn("Status mismatch! {}", response.getStatus());
                }
                
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        } else {
            logger.info("file already downloaded");
        }
    }
    
    public File getRecording(Calendar calStart, Calendar calEnd, long delta) throws VideoNotAvailableException {
        Collection<String> parts = new LinkedList<>();
        JSONObject json = new JSONObject();
        if (calStart.after(calEnd)) {
            Calendar temp = calEnd;
            calEnd = calStart;
            calStart = temp;
        }
        StringBuilder concat = new StringBuilder();
        JSONArray array = new JSONArray();
        json.put("cmd", "Search");
        json.put("action", 1);
        JSONObject param = new JSONObject();
        JSONObject search = new JSONObject();
        
        search.put("channel", 0);
        search.put("onlyStatus", 0);
        search.put("streamType", "main");
        //from.getMinutes()
        Calendar videostart = null;
        Calendar videoend = null;
        
        search.put("EndTime", convertToJSON(calEnd));
        search.put("StartTime", convertToJSON(calStart));
        param.put("Search", search);
        json.put("param", param);
        array.put(json);
        logger.info("got json {}", json);
        String pattern = "yyyy-MM-dd_HH-mm-ss";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
        String timePattern = "HH:mm:ss";
        String filename = this.name + "_" + simpleDateFormat.format(calStart.getTime());
        
        File jsonfile = new File(mydir + "/" + filename + ".json");
        File hashfile = new File(mydir + "/" + filename + ".txt");
        
        File outfile = new File(mydir + "/" + filename + ".mp4");
        if (outfile.exists()) {
            outfile.delete();
        }
        if (!outfile.exists() || !jsonfile.exists() || !hashfile.exists()) {
            Request request = createRequest(HttpMethod.GET, null, array);
            try {
                ContentResponse response = request.send();
                String content = response.getContentAsString();
                JSONArray arr = new JSONArray(content);
                for (int i = 0; i < arr.length(); i++) {
                    try {
                        JSONObject o = arr.getJSONObject(i);
                        if (o.has("value")) {
                            JSONArray files = o.getJSONObject("value").getJSONObject("SearchResult").getJSONArray("File");
                            for (int j = 0; j < files.length(); j++) {
                                JSONObject f = files.getJSONObject(j);
                                logger.info("found file to get {}", f);
                                this.download(f.getString("name"));
                                parts.add(f.getString("name"));
                                Calendar s = convertToCal(f.getJSONObject("StartTime"));
                                Calendar e = convertToCal(f.getJSONObject("EndTime"));
                                if (videoend == null || e.after(videoend)) {
                                    videoend = e;
                                }
                                if (videostart == null || s.before(videostart)) {
                                    videostart = s;
                                }
                            }
                            logger.info("download finished - downloaded {} files", files.length());
                        } else {
                            logger.warn("NO DOWNLOAD!");
                            
                        }
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            if (videoend == null) {
                logger.warn("COULD NOT DOWNLOAD VIDEOS!");
                throw new VideoNotAvailableException();
            }
            if (videostart == null) {
                logger.warn("COULD NOT DOWNLOAD VIDEOS!");
                throw new VideoNotAvailableException();
            }
            videostart.add(Calendar.MILLISECOND, (int) -delta);
            videoend.add(Calendar.MILLISECOND, (int) -delta);
            
            KosmosFileUtils.writeToFile(hashfile, concat.toString());
            Locale locale = new Locale("en", "US");
            DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.FULL, locale);
            
            logger.info("startdate {} enddate {}", dateFormat.format(videostart.getTime()), dateFormat.format(videoend.getTime()));
            logger.info("wanted {} enddate {}", dateFormat.format(calStart.getTime()), dateFormat.format(calEnd.getTime()));
            long startDelta = calStart.getTimeInMillis() - videostart.getTimeInMillis();
            long wantedDuration = calEnd.getTimeInMillis() - calStart.getTimeInMillis();
            
            
            try {
                FFMPEGWrapper.mergeVideos(parts, outfile, startDelta, wantedDuration);
            } catch (IOException e) {
                e.printStackTrace();
            }
            json = new JSONObject();
            json.put("videostart", videostart.getTimeInMillis() - delta);
            json.put("videoend", videoend.getTimeInMillis() - delta);
            KosmosFileUtils.writeToFile(jsonfile, json.toString());
            
        } else {
            json = new JSONObject(KosmosFileUtils.readFile(jsonfile));
            videoend = Calendar.getInstance();
            videoend.setTimeInMillis(json.getLong("videoend") - delta);
            videostart = Calendar.getInstance();
            videostart.setTimeInMillis(json.getLong("videostart") - delta);
        }
        
        
        logger.info("download done");
        return outfile;
    }
    
    @Override
    public byte[] getSnapshot() {
        byte[] bytes = null;
        Request req = this.createRequest("&cmd=Snap&channel=0", HttpMethod.GET, null, (JSONArray) null);
        try {
            InputStreamResponseListener listener = new InputStreamResponseListener();
            
            req.send(listener);
            Response response = listener.get(20, TimeUnit.SECONDS);
            
            if (response.getStatus() == 200) {
                // Obtain the input stream on the response content
                try (InputStream input = listener.getInputStream()) {
                    bytes = input.readAllBytes();
                    
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                logger.warn("Status mismatch! {}", response.getStatus());
            }
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    @Override
    public void startRecording() {

    }

    @Override
    public void stopRecording() {

    }

    @Override
    public String getName() {
        return this.name;
    }

    public void stop() {
        try {
            this.client.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}