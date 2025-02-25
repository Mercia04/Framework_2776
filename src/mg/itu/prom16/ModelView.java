package mg.itu.prom16;

import java.util.HashMap;

public class ModelView {
    private String url;
    private String urlError;

    public String getUrlError() {
        return urlError;
    }

    public void setUrlError(String urlError) {
        this.urlError = urlError;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    private HashMap<String, Object> data = new HashMap<>();

    public HashMap<String, Object> getData() {
        return data;
    }

    public void setData(HashMap<String, Object> data) {
        this.data = data;
    }

    public void addObject(String key, Object object) {
        this.data.put(key, object);
    }
}
