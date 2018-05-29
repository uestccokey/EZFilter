package cn.ezandroid.ezfilter.demo.render.particle.util;

import android.content.Context;
import android.content.res.Resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TextResourceReader {

    public static String readTextFileFromResource(Context context, int resourceId) {
        StringBuilder body = new StringBuilder();
        try {
            InputStream is = context.getResources().openRawResource(resourceId);
            InputStreamReader isReader = new InputStreamReader(is);
            BufferedReader buffReader = new BufferedReader(isReader);
            String nextLine;
            while ((nextLine = buffReader.readLine()) != null) {
                body.append(nextLine);
                body.append("\n");
            }
        } catch (IOException ioe) {
            throw new RuntimeException("Could not open resource: " + resourceId);
        } catch (Resources.NotFoundException nfe) {
            throw new RuntimeException("Resource not found: " + resourceId, nfe);
        }
        return body.toString();
    }
}
