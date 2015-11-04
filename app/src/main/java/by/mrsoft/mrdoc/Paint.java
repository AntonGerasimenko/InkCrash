package by.mrsoft.mrdoc;

import com.radaee.pdf.Document;
import com.radaee.pdf.Page;

public final class Paint {

    public  static int PAGE_NUM = 0;

    public static void INK(Document document, byte[] data) {

        Page page = document.GetPage(PAGE_NUM);
        if (page != null) {

            page.ObjsStart();

            InkConverter.addInkAnnotToPage(data, page, document.GetPageHeight(PAGE_NUM));

            page.Close();
            document.Save();
        }
    }

    public static boolean byteNullAnalyze(byte[] ink) {

        byte [] buff = new byte[10];
        System.arraycopy(ink, ink.length - 10, buff, 0, 10);

        int count = 0;
        for (byte one:buff) {
            if (one == 0) count++;
        }
        return count != 10;
    }
}
