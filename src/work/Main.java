package work;

import com.qiniu.api.auth.AuthException;
import com.qiniu.api.auth.digest.Mac;
import com.qiniu.api.config.Config;
import com.qiniu.api.rs.GetPolicy;
import com.qiniu.api.rs.RSClient;
import com.qiniu.api.rs.URLUtils;
import com.qiniu.api.rsf.ListItem;
import com.qiniu.api.rsf.ListPrefixRet;
import com.qiniu.api.rsf.RSFClient;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String ACCESS_KEY = "zVxhvVY8ggEUftanwKVdmqNLvoi2IXrOTZG9NwMT";
    private static final String SECRET_KEY = "IIiL9fdiVOHqmiixrF6NYY-pRMVU5Gjo5UfnYPUE";
    private static String[] buckets = {
                "deliver-app-log",
                "thunder-app-log",
                "windmap-log",
                "dev-deliver-app-log",
                "dev-thunder-app-log",
                "dev-windmap-app-log"
            };
    private Mac mac;
    private RSFClient rsfClient;
    private RSClient rsClient;

    public static void main(String[] args) {
        Config.ACCESS_KEY = ACCESS_KEY;
        Config.SECRET_KEY = SECRET_KEY;

        for (String bucket : buckets) {
            new Main().StartWork(bucket);
        }
    }

    public void StartWork(String bucket) {
        if (bucket == null || bucket.isEmpty()) {
            return;
        }
        System.out.println("-----" + bucket + "-----");
        mac = new Mac(Config.ACCESS_KEY, Config.SECRET_KEY);
        rsfClient = new RSFClient(mac);
        rsClient = new RSClient(mac);
        String marker = "";

        List<String> keyList = new ArrayList<String>();
        ListPrefixRet ret = null;
        while (true) {
            ret = rsfClient.listPrifix(bucket, "", marker, 100);
            marker = ret.marker;
            for (ListItem item : ret.results) {
                keyList.add(item.key);
            }
            if (!ret.ok()) {
                break;
            }
        }
        for (String key : keyList) {
            downLoadFile(bucket, key);
            break;
        }
    }

    private void downLoadFile(String bucket, String key) {
        String domain = bucket + ".qiniudn.com";
        try {
            String parts[] = key.split("_");
            if (parts.length < 4) {
                return;
            }

            String fileName = parts[0] + "_" + parts[2] + "_" + parts[3];
            String fileDate = parts[1];
            File dir = new File("../data/" + bucket + "/" + fileDate);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir.getAbsolutePath() + "/" + fileName);
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();

            String baseUrl = URLUtils.makeBaseUrl(domain, key);
            GetPolicy getPolicy = new GetPolicy();
            String downloadUrl = getPolicy.makeRequest(baseUrl, mac);
            System.out.println(key + "下载开始");
            FileUtils.copyURLToFile(new URL(downloadUrl), file);
            System.out.println(key + "下载完成");
//            rsClient.delete(bucket, key);
//            System.out.println(key + "删除完成");
        } catch (EncoderException e) {
            e.printStackTrace();
        } catch (AuthException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
