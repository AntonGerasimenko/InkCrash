package by.mrsoft.mrdoc;


import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.radaee.pdf.Document;
import com.radaee.pdf.Global;
import com.radaee.pdf.Page;
import com.radaee.view.PDFVPage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {

    PDFReader reader;

    final static String pdfName = "simple.pdf";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Global.Init(this);

        setContentView(R.layout.content_main);

        reader = (PDFReader) findViewById(R.id.view);

        File file  = getFile(pdfName);

        if (!file.isFile()) {

            copyAssets(pdfName);
        }

        String path  = file.getPath();

        Document document = new Document();
        document.Open(path,"");
        document.SetCache(Global.tmp_path + "/temp.dat");
        reader.PDFOpen(document, false, new PDFReader.PDFReaderListener() {
            @Override
            public void OnPageModified(int pageno) {

            }

            @Override
            public void OnPageChanged(int pageno) {

            }

            @Override
            public void OnAnnotClicked(PDFVPage vpage, Page.Annotation annot) {

            }

            @Override
            public void OnSelectEnd(String text) {

            }

            @Override
            public void OnOpenURI(String uri) {

            }

            @Override
            public void OnOpenJS(String js) {

            }

            @Override
            public void OnOpenMovie(String path) {

            }

            @Override
            public void OnOpenSound(int[] paras, String path) {

            }

            @Override
            public void OnOpenAttachment(String path) {

            }

            @Override
            public void OnOpen3D(String path) {

            }
        });

    }


    private File getFile(String fileName) {
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        return  new File(downloadDir, fileName);
    }


    private void copyAssets(String filename) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = getAssets().open(filename);
            File outFile =  getFile(filename);
            outFile.createNewFile();
            out = new FileOutputStream(outFile);
            copyFile(in, out);
        } catch(IOException e) {
            Log.e("tag", "Failed to copy asset file: " + filename, e);
        }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    // NOOP
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }
}
