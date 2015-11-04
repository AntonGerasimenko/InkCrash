package by.mrsoft.mrdoc;

import java.security.InvalidParameterException;



public class ByteBufferStream {
    private byte[] data;
    private int curPosition;
    private int realSize;
    private int order;

    public static final int LITTLE_ENDIAN = 1;
    public static final int BIG_ENDIAN = 2;


    private void create(byte data[], int order) {
        this.data = data;
        switch (order) {
            case LITTLE_ENDIAN:
            case BIG_ENDIAN:
                break;
            default:
                throw new InvalidParameterException("Order can be LITTLE_ENDIAN or BIG_ENDIAN");
        }
        this.order = order;
        curPosition = 0;
        realSize = 0;
    }

    public ByteBufferStream(int size) {
        byte newData[] = new byte[size];
        create(newData, LITTLE_ENDIAN);
    }

    public ByteBufferStream(int size, int order) {
        byte newData[] = new byte[size];
        create(newData, order);
    }

    public ByteBufferStream(byte buffer[], int order) {
        create(buffer, order);
        realSize = buffer.length;
    }


    public int writeByte(byte src) {
        data[curPosition] = src;
        curPosition++;
        realSize++;
        return curPosition;
    }

    public byte readByte() {
        curPosition++;
        return data[curPosition - 1];
    }

    public int writeInt(int src) {
        switch (order) {
            case LITTLE_ENDIAN:
                data[curPosition+0] = (byte) ((src)       & 0xff);
                data[curPosition+1] = (byte) ((src >> 8 ) & 0xff);
                data[curPosition+2] = (byte) ((src >> 16) & 0xff);
                data[curPosition+3] = (byte) ((src >> 24) & 0xff);
                break;
            case BIG_ENDIAN:
                data[curPosition+3] = (byte) ((src)       & 0xff);
                data[curPosition+2] = (byte) ((src >> 8 ) & 0xff);
                data[curPosition+1] = (byte) ((src >> 16) & 0xff);
                data[curPosition+0] = (byte) ((src >> 24) & 0xff);
                break;
        }

        curPosition += 4;
        realSize += 4;

        return curPosition;
    }

    public int readInt() {
        int result = 0;
        switch (order) {
            case LITTLE_ENDIAN:
                result =(((int)data[curPosition+0] & 0xff) +
                        (((int)data[curPosition+1] & 0xff) << 8 ) +
                        (((int)data[curPosition+2] & 0xff) << 16) +
                        (((int)data[curPosition+3] & 0xff) << 24));
                break;
            case BIG_ENDIAN:
                result =(((int)data[curPosition+3] & 0xff) +
                        (((int)data[curPosition+2] & 0xff) << 8 ) +
                        (((int)data[curPosition+1] & 0xff) << 16) +
                        (((int)data[curPosition+0] & 0xff) << 24));
                break;
        }

        curPosition += 4;

        return result;
    }


    public int writeColor(int color) {
        //В данных - RGBA
        //Либа просит - ARGB
        data[curPosition+0] = (byte)((color >> 16) & 0xff);
        data[curPosition+1] = (byte)((color >> 8 ) & 0xff);
        data[curPosition+2] = (byte)((color      ) & 0xff);
        data[curPosition+3] = (byte)((color >> 24) & 0xff);

        curPosition += 4;
        realSize += 4;

        return curPosition;
    }

    public int readColor() {
        int color = 0;

        color = (((int)data[curPosition+0] & 0xff) << 16) +
                (((int)data[curPosition+1] & 0xff) << 8 ) +
                (((int)data[curPosition+2] & 0xff)      ) +
                (((int)data[curPosition+3] & 0xff) << 24);
        curPosition += 4;

        return color;
    }

    public int writeDouble(double src){
        long value = Double.doubleToLongBits(src);
        switch (order) {
            case LITTLE_ENDIAN:
                data[curPosition+0] = (byte)((value      ) & 0xff);
                data[curPosition+1] = (byte)((value >> 8 ) & 0xff);
                data[curPosition+2] = (byte)((value >> 16) & 0xff);
                data[curPosition+3] = (byte)((value >> 24) & 0xff);
                data[curPosition+4] = (byte)((value >> 32) & 0xff);
                data[curPosition+5] = (byte)((value >> 40) & 0xff);
                data[curPosition+6] = (byte)((value >> 48) & 0xff);
                data[curPosition+7] = (byte)((value >> 56) & 0xff);
                break;
            case BIG_ENDIAN:
                data[curPosition+7] = (byte)((value      ) & 0xff);
                data[curPosition+6] = (byte)((value >> 8 ) & 0xff);
                data[curPosition+5] = (byte)((value >> 16) & 0xff);
                data[curPosition+4] = (byte)((value >> 24) & 0xff);
                data[curPosition+3] = (byte)((value >> 32) & 0xff);
                data[curPosition+2] = (byte)((value >> 40) & 0xff);
                data[curPosition+1] = (byte)((value >> 48) & 0xff);
                data[curPosition+0] = (byte)((value >> 56) & 0xff);
                break;
        }

        curPosition += 8;
        realSize += 8;

        return curPosition;
    }

    public double readDouble() {
        long result = 0;

        switch (order) {
            case LITTLE_ENDIAN:
                result = (
                        (((long)data[curPosition+0] & 0xff) << 0  ) +
                        (((long)data[curPosition+1] & 0xff) << 8  ) +
                        (((long)data[curPosition+2] & 0xff) << 16 ) +
                        (((long)data[curPosition+3] & 0xff) << 24 ) +
                        (((long)data[curPosition+4] & 0xff) << 32 ) +
                        (((long)data[curPosition+5] & 0xff) << 40 ) +
                        (((long)data[curPosition+6] & 0xff) << 48 ) +
                        (((long)data[curPosition+7] & 0xff) << 56 )
                );
                break;
            case BIG_ENDIAN:
                result = (
                        (((long)data[curPosition+7] & 0xff) << 0  ) +
                        (((long)data[curPosition+6] & 0xff) << 8  ) +
                        (((long)data[curPosition+5] & 0xff) << 16 ) +
                        (((long)data[curPosition+4] & 0xff) << 24 ) +
                        (((long)data[curPosition+3] & 0xff) << 32 ) +
                        (((long)data[curPosition+2] & 0xff) << 40 ) +
                        (((long)data[curPosition+1] & 0xff) << 48 ) +
                        (((long)data[curPosition+0] & 0xff) << 56 )
                );
                break;
        }

        curPosition += 8;

       // EOSmobileUtils.getInstance().writeLog("Inc FAULT", "Order = " + order + " result = "+ result + "curPos = "+curPosition);
        return Double.longBitsToDouble(result);
    }

    //Возврат массивов байт
    //Возвращает массив или его копию
    //(в случае, если массив не был заполнен полностью)
    public byte[] getBytes() {
        byte result[];
        if (realSize == data.length) {
            result = data;
        } else {
            result = new byte[realSize];
            System.arraycopy(data, 0, result, 0, realSize);
        }

        return result;
    }
}
