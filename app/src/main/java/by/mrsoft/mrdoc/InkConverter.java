package by.mrsoft.mrdoc;

import android.util.Log;

import com.radaee.pdf.Global;
import com.radaee.pdf.Ink;
import com.radaee.pdf.Matrix;
import com.radaee.pdf.Page;
import com.radaee.pdf.Path;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;



/**
 * Created by CyrillCheushev on 09.12.2014.
 */
public class InkConverter {

    public static final float SCALE_COEFF = 0.75f;

    private static int MOVE_TO = 0;
    private static int LINE_TO = 1;
    private static int CURVE_TO = 2;
    private static int CLOSE = 3;

    static public boolean addInkAnnotToPage(byte[] data, Page page, float pageHeight) {
        Matrix mat = new Matrix(SCALE_COEFF, 0, 0, -SCALE_COEFF, 0, pageHeight);
        ByteBufferStream byteStream = new ByteBufferStream(data, ByteBufferStream.LITTLE_ENDIAN);

        int objectsCount = byteStream.readInt();

        Ink curInk = null;

        for (int curObject = 0; curObject != objectsCount; curObject++) {
            Global.inkColor = byteStream.readColor();

            float width = (float) byteStream.readDouble();
            float height = (float) byteStream.readDouble();

            if (curInk == null) {
                curInk = new Ink(width);
            }

            int pointsCount = byteStream.readInt();


            if (pointsCount > 0) {

                float x = (float) byteStream.readDouble();
                float y = (float) byteStream.readDouble();
                curInk.OnDown(x, y);
                int curPoint;
                for (curPoint = 1; curPoint < pointsCount; curPoint++) {

                    x = (float) byteStream.readDouble();
                    y = (float) byteStream.readDouble();

                    Log.d("Ink", " point : " + curPoint + " x= " + x + " y= " + y);

                    curInk.OnMove(x, y);
                }
            }
        }
        if (curInk != null ) {

            mat.TransformInk(curInk);
            page.AddAnnotInk(curInk);
            Log.d("Ink", " Add");
            return true;
        }
       return false;
    }

    static public byte[] inkToByte(Page.Annotation annotation, float pageHeight) {
        Path path = annotation.GetInkPath();

        Matrix mat = new Matrix(1.0f / SCALE_COEFF, 0, 0, -(1.0f / SCALE_COEFF), 0, pageHeight * (1.0f / SCALE_COEFF));
        mat.TransformPath(path);

        int objectsCount = 0;

        int nodesCount = path.GetNodeCount();
        float coordinates[] = new float[2];

        //Подсчет количества "объектов" для результирующего сообщения
        //для определения точного размера выходного массива
        LinkedList<Integer> pointsCounts = new LinkedList<Integer>();
        int curPointsCount = 1;
        for (int i = 0; i < nodesCount; i++) {
            int type = path.GetNode(i, coordinates);


            if (type == MOVE_TO) {

                Log.d("Ink", " Move to  " + coordinates[0] + "/" + coordinates[1]);
                objectsCount++;
                if (curPointsCount != 1) {
                    pointsCounts.add(curPointsCount);
                }
                curPointsCount = 1;
            }

            if (type == LINE_TO) {
                Log.d("Ink", " Line to  " + coordinates[0] + "/" + coordinates[1]);
                curPointsCount++;
            }
        }
        if (curPointsCount != 1) {
            pointsCounts.add(curPointsCount);
        }
        Log.d("Ink", "objectsCount/nodesCount" + objectsCount + "/" + nodesCount);
        ByteBufferStream byteStream = new ByteBufferStream(
                4 + objectsCount * (4 + 2 * 8 + 4) + nodesCount * 2 * 8
                , ByteBufferStream.LITTLE_ENDIAN);

        byteStream.writeInt(objectsCount);

        int color = annotation.GetStrokeColor();
        float width = annotation.GetStrokeWidth();
        for (int i = 0; i < nodesCount; i++) {
            int type = path.GetNode(i, coordinates);

            if (type == MOVE_TO) {
                byteStream.writeColor(color);
                byteStream.writeDouble((double) width);
                byteStream.writeDouble((double) width);
                byteStream.writeInt(pointsCounts.poll());
            }

            if ((type == LINE_TO) || (type == MOVE_TO)) {
                byteStream.writeDouble((double) coordinates[0]);
                byteStream.writeDouble((double) coordinates[1]);
            }
        }

        return byteStream.getBytes();
    }

    private void addGraphAnnotToPage(byte[] data, Page page, float pageHeight) {
        Matrix mat = new Matrix(1, 0, 0, -1, 0, pageHeight);

        int curPosition = 0;

        int objectsCount = (data[curPosition]) +
                (data[curPosition + 1] << 8) +
                (data[curPosition + 2] << 16) +
                (data[curPosition + 3] << 24);
        curPosition += 4;

        for (int curObject = 0; curObject != objectsCount; curObject++) {

            Path curPath = new Path();

            int color;
            //В данных - RGBA
            //Либа просит - ARGB
            color = (data[curPosition] << 16)
                    + (data[curPosition + 1] << 8)
                    + (data[curPosition + 2])
                    + (data[curPosition + 3] << 24);
            curPosition += 4;


            /*
            double width;
            double height;
            width = dataStream.readDouble();
            height = dataStream.readDouble();
            */
            curPosition += 16;

            int pointsCount;
            pointsCount = (data[curPosition]) +
                    (data[curPosition + 1] << 8) +
                    (data[curPosition + 2] << 16) +
                    (data[curPosition + 3] << 24)
            ;
            curPosition += 4;

            if (pointsCount > 0) {
                curPath.MoveTo(
                        (float) ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getDouble(curPosition)
                        , (float) ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getDouble(curPosition + 8)
                );
                curPosition += 16;
                for (int curPoint = 1; curPoint < pointsCount; curPoint++) {
                    curPath.LineTo(
                            (float) ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getDouble(curPosition)
                            , (float) ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getDouble(curPosition + 8)
                    );
                    curPosition += 16;
                }
            }
            curPath.ClosePath();
            page.AddAnnotGlyph(mat, curPath, color, true);
        }
    }
}
