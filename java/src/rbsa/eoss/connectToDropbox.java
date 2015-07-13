/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package rbsa.eoss;

import com.dropbox.core.*;
import java.io.*;
import java.util.Locale;
import rbsa.eoss.local.Params;

/**
 *Use to connect app to Dropbox account. I think you only need to do this once.
 * @author nozomihitomi
 */
public class connectToDropbox {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws DbxException, IOException {
        //gain access to Dropbox
        DbxAppInfo appInfo = new DbxAppInfo("501z5dhek2czvcm", "q21mrispb7be3oz");
        DbxRequestConfig config = new DbxRequestConfig(
            "JavaTutorial/1.0", Locale.getDefault().toString());
        String accessToken = "ZvDr5rq4iJMAAAAAAAAGGJJdaS5IX23e3xNugpvHdkiNEEC4xlieCVwEFsKn_EOk";
        DbxClient client = new DbxClient(config, accessToken);
        System.out.println("Connected to Dropbox. Linked account: " + client.getAccountInfo().displayName);
        
        
//        File inputFile = new File("/Users/nozomihitomi/Desktop/test.txt");
//        FileInputStream inputStream = new FileInputStream(inputFile);
//        try {
//            DbxEntry.File uploadedFile = client.uploadFile("/magnum-opus.txt",
//                DbxWriteMode.add(), inputFile.length(), inputStream);
//            System.out.println("Uploaded: " + uploadedFile.toString());
//        } finally {
//            inputStream.close();
//        }
//
//        DbxEntry.WithChildren listing = client.getMetadataWithChildren("/");
//        System.out.println("Files in the root path:");
//        for (DbxEntry child : listing.children) {
//            System.out.println("	" + child.name + ": " + child.toString());
//        }

        FileOutputStream outputStream = new FileOutputStream("magnum-opus.txt");
        try {
            DbxEntry.File downloadedFile = client.getFile("/test/magnum-opus.txt", null,
                outputStream);
            System.out.println("Metadata: " + downloadedFile.toString());
        } finally {
            outputStream.close();
        }
    }
    
}
