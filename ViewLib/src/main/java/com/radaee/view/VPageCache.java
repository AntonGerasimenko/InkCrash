package com.radaee.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;

import com.radaee.pdf.Document;
import com.radaee.pdf.Matrix;
import com.radaee.pdf.Page;

/**
 * Created by radaee on 2015/10/29.
 */
public class VPageCache
{
    private Document m_doc;
    private VBlock m_blks[];
    private float m_scale;
    private int m_size;
    private int m_pageno;
    private int m_dir;
    private Bitmap.Config m_bmp_format = Bitmap.Config.ARGB_8888;
    static private Paint m_paint;
    public class VBlock
    {
        VBlock()
        {
            if(m_paint == null)
            {
                m_paint = new Paint();
                m_paint.setStyle(Paint.Style.FILL);
                m_paint.setColor(-1);
            }
        }
        VBlock(VBlock src)
        {
            if(m_paint == null)
            {
                m_paint = new Paint();
                m_paint.setStyle(Paint.Style.FILL);
                m_paint.setColor(-1);
            }
            x = src.x;
            y = src.y;
            size = src.size;
        }
        private void Render(Document doc, int pno, Matrix mat, int w, int h)
        {
            page = doc.GetPage(pno);
            Bitmap bmp2 = Bitmap.createBitmap(w, h, m_bmp_format);
            bmp2.eraseColor(-1);
            page.RenderToBmp(bmp2, mat);
            if(status != -1)
            {
                status = 2;//set finished.
                bmp = bmp2;
            }
            else
            {
                bmp2.recycle();
                bmp = null;
            }
        }
        final public void Render()
        {
            if(m_dir == 0)//vertical
            {
                Matrix mat = new Matrix(m_scale, - m_scale, 0, y + size);
                Render(m_doc, m_pageno, mat, m_size, size);
                mat.Destroy();
            }
            else//horizon
            {
                Matrix mat = new Matrix(m_scale, - m_scale, 0, m_size);
                Render(m_doc, m_pageno, mat, size, m_size);
                mat.Destroy();
            }
        }
        final protected void Cancel()
        {
            if(status != 2 && status != -1)//set cancel
            {
                if(page != null)
                    page.RenderCancel();
                status = -1;
            }
        }
        final protected boolean Draw(Canvas canvas, Rect src, Rect dst)
        {
            if(bmp != null)
            {
                canvas.drawBitmap(bmp, src, dst, null);
                return true;
            }
            else
            {
                return status == 0;
            }
        }
        final public void Reset()
        {
            if(page != null) page.Close();
            if(bmp != null) bmp.recycle();
            page = null;
            bmp = null;
        }
        @Override
        protected void finalize() throws Throwable
        {
            if(page != null) page.Close();
            if(bmp != null) bmp.recycle();
            super.finalize();
        }
        int x;
        int y;
        int size;
        int status;
        Page page;
        Bitmap bmp;
    }
    protected VPageCache(Document doc, int pageno, int size, Bitmap.Config bmp_format)
    {
        m_doc = doc;
        m_pageno = pageno;
        m_size = size;
        m_bmp_format = bmp_format;
        init();
    }
    private void init()
    {
        float w = m_doc.GetPageWidth(m_pageno);
        float h = m_doc.GetPageHeight(m_pageno);
        if(w > h)
        {
            m_dir = 1;
            m_scale = m_size / h;
            int total = (int)(m_scale * w);
            int blocks = total / m_size;
            m_blks = new VBlock[blocks];
            for(int ib = 0; ib < blocks; ib++)
            {
                VBlock blk = new VBlock();
                m_blks[ib] = blk;
                blk.x = ib * m_size;
                blk.y = 0;
                blk.size = m_size;
                blk.status = 0;
                blk.bmp = null;
            }
            m_blks[blocks - 1].size = total - (m_blks[blocks - 1].x);
        }
        else
        {
            m_dir = 0;
            m_scale = m_size / w;
            int total = (int)(m_scale * h);
            int blocks = total / m_size;
            m_blks = new VBlock[blocks];
            for(int ib = 0; ib < blocks; ib++)
            {
                VBlock blk = new VBlock();
                m_blks[ib] = blk;
                blk.x = 0;
                blk.y = ib * m_size;
                blk.size = m_size;
                blk.status = 0;
                blk.bmp = null;
            }
            m_blks[blocks - 1].size = total - (m_blks[blocks - 1].y);
        }
    }

    final protected VBlock blk_render(int blk)
    {
        //Log.d("PDF CACHE", "render page:" + m_pageno);
        VBlock dst = m_blks[blk];
        if(dst.status == 1 || dst.status == 2) return null;
        if(dst.status == -1)
        {
            dst = new VBlock(dst);
            m_blks[blk] = dst;
        }
        dst.status = 1;
        return dst;
    }
    final protected VBlock blk_cancel(int blk)
    {
        //Log.d("PDF CACHE", "cancel page:" + m_pageno);
        VBlock dst = m_blks[blk];
        if(dst.status == 1)
        {
            dst.Cancel();
            m_blks[blk] = new VBlock(dst);
            return dst;
        }
        else if(dst.status == 2)
        {
            m_blks[blk] = new VBlock(dst);
            return dst;
        }
        else
            return null;
    }

    final protected int blk_get(float pdfx, float pdfy)
    {
        int blk;
        if(m_dir == 0)
            blk = (int)((m_doc.GetPageHeight(m_pageno) - pdfy) * m_scale) / m_size;
        else
            blk = (int)(pdfx * m_scale) / m_size;
        if(blk < 0) blk = 0;
        if(blk >= m_blks.length) blk = m_blks.length - 1;
        return blk;
    }

    final protected int blk_get_count()
    {
        return m_blks.length;
    }
    final protected boolean blk_rendered()
    {
        int cnt = m_blks.length;
        for(int cur = 0; cur < cnt; cur++)
        {
            VBlock blk = m_blks[cur];
            if(!(blk.status == 2 || blk.status == 0)) return false;
        }
        return true;
    }
    protected boolean blk_draw(Canvas canvas, float pdfx1, float pdfy1, float pdfx2, float pdfy2, int x, int y, float scale)
    {
        int blk1 = blk_get(pdfx1, pdfy1);
        int blk2 = blk_get(pdfx2, pdfy2);
        Rect src = new Rect();
        Rect dst = new Rect();
        VBlock blk;
        float mul2 = scale / m_scale;//we hate divide OP
        if(m_dir == 0)//vertical
        {
            src.left = (int)(pdfx1 * m_scale);
            src.right = (int)(pdfx2 * m_scale);
            dst.left = x;
            dst.right = dst.left + (int)((pdfx2 - pdfx1) * scale);
            while(blk1 < blk2)
            {
                blk = m_blks[blk1];
                src.top = (int)((m_doc.GetPageHeight(m_pageno) - pdfy1) * m_scale) - blk.y;
                src.bottom = blk.size;
                dst.top = y;
                dst.bottom = dst.top + (int)(src.height() * mul2);
                if( !blk.Draw(canvas, src, dst) ) return false;
                pdfy1 = m_doc.GetPageHeight(m_pageno) - ((blk.y + blk.size)  / m_scale);
                y = dst.bottom;
                blk1++;
            }
            blk = m_blks[blk1];
            src.top = (int)((m_doc.GetPageHeight(m_pageno) - pdfy1) * m_scale) - blk.y;
            src.bottom = blk.size;
            dst.top = y;
            dst.bottom = dst.top + (int)(src.height() * mul2);
            return blk.Draw(canvas, src, dst);
        }
        else//horizon
        {
            src.top = (int)((m_doc.GetPageHeight(m_pageno) - pdfy1) * m_scale);
            src.bottom = (int)((m_doc.GetPageHeight(m_pageno) - pdfy2) * m_scale);
            dst.top = y;
            dst.bottom = dst.top + (int)((pdfy1 - pdfy2) * scale);
            while(blk1 < blk2)
            {
                blk = m_blks[blk1];
                src.left = (int)(pdfx1 * m_scale) - blk.x;
                src.right = blk.size;
                dst.left = x;
                dst.right = dst.left + (int)(src.width() * mul2);
                if( !blk.Draw(canvas, src, dst) ) return false;
                pdfx1 = (blk.x + blk.size)  / m_scale;
                x = dst.right;
                blk1++;
            }
            blk = m_blks[blk1];
            src.left = (int)(pdfx1 * m_scale) - blk.x;
            src.right = blk.size;
            dst.left = x;
            dst.right = dst.left + (int)(src.width() * mul2);
            return blk.Draw(canvas, src, dst);
        }
    }
}
