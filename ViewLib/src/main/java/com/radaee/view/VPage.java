package com.radaee.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.radaee.pdf.BMP;
import com.radaee.pdf.Document;
import com.radaee.pdf.Matrix;

public class VPage
{
	private VCache m_caches[][];
	private Bitmap m_zoom_bmp;
	private Document m_doc;
	private int m_pageno;
	private int m_x;
	private int m_y;
	private int m_w;
	private int m_h;
	private float m_scale = 1;
    private VPageCache m_cache;
	static protected int CLIP_W;
	static protected int CLIP_H;
	static void SetConfigure(Context context)
	{
		if(CLIP_W <= 0 || CLIP_H <=0)
		{
			WindowManager manager = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
			DisplayMetrics dm = new DisplayMetrics();
			manager.getDefaultDisplay().getMetrics(dm);
			int width = dm.widthPixels;
			int height = dm.heightPixels;
			if(width > height) width = height;
			CLIP_W = width;
			CLIP_H = width;
		}
	}
    protected class VPageRenderResult
    {
        VPageRenderResult(int cols, int rows)
        {
            m_flags = new boolean[cols][rows];
        }
        private int bmpw;
        private int bmph;
        private int xcnt;
        private int ycnt;
        private int xb0;
        private int yb0;
        private int x0;
        private int y0;
        private boolean m_flags[][];
    }
	protected VPage(Document doc, int pageno, Config bmp_format)
	{
		m_doc = doc;
		m_pageno = pageno;
		m_caches = null;
		m_zoom_bmp = null;
		m_x = 0;
		m_y = 0;
		m_w = 0;
		m_h = 0;
        m_cache = new VPageCache(doc, pageno, CLIP_W * 2 / 3, bmp_format);
	}
	public final int GetPageNo(){return m_pageno;}
	protected final int GetX() {return m_x;}
	protected final void SetX(int x){m_x = x;}
	protected final int GetY() {return m_y;}
	protected final int GetWidth() {return m_w;}
	protected final int GetHeight() {return m_h;}
	protected final float GetPDFX(int vx){return (vx - m_x)/m_scale;}
	protected final float GetPDFY(int vy){return (m_y + m_h - vy)/m_scale;}
	public final int GetVX(float pdfx){return (int)(pdfx * m_scale) + m_x;}
	public final int GetVY(float pdfy){return m_h + m_y - (int)(pdfy * m_scale);}
    protected int LocVert(int y, int gap_half)
    {
        if( y < m_y - gap_half ) return -1;
        else if( y > m_y + m_h + gap_half ) return 1;
        else return 0;
    }
    protected int LocHorz(int x, int gap_half)
    {
        if( x < m_x - gap_half ) return -1;
        else if( x > m_x + m_w + gap_half ) return 1;
        else return 0;
    }
	private void blocks_destroy(VThread thread)
	{
		if(m_caches == null) return;
		int xcur = 0;
		int ycur = 0;
		int xcnt = m_caches.length;
		int ycnt = m_caches[0].length;
		for(ycur = 0; ycur < ycnt; ycur++)
		{
			for(xcur = 0; xcur < xcnt; xcur++)
			{
				thread.end_render(m_caches[xcur][ycur]);
			}
		}
		m_caches = null;
	}
	private void blocks_create()
	{
		int xcnt = m_w / CLIP_W;
		int ycnt = m_h / CLIP_H;
		int xtail = m_w % CLIP_W;
		int ytail = m_h % CLIP_H;
		if(xtail > (CLIP_W>>1)) xcnt++;
		if(ytail > (CLIP_H>>1)) ycnt++;
		if(xcnt <= 0) xcnt = 1;
		if(ycnt <= 0) ycnt = 1;
		m_caches = new VCache[xcnt][ycnt];
		int xcur;
		int ycur;
		int y = 0;
		int x;
		for( ycur = 0; ycur < ycnt - 1; ycur++ )
		{
			x = 0;
			for(xcur = 0; xcur < xcnt - 1; xcur++)
			{
				m_caches[xcur][ycur] = new VCache(m_doc, m_pageno, m_scale, x, y, CLIP_W, CLIP_H);
				x += CLIP_W;
			}
			m_caches[xcur][ycur] = new VCache(m_doc, m_pageno, m_scale, x, y, m_w - x, CLIP_H);
			y += CLIP_H;
		}
		x = 0;
		for(xcur = 0; xcur < xcnt - 1; xcur++)
		{
			m_caches[xcur][ycur] = new VCache(m_doc, m_pageno, m_scale, x, y, CLIP_W, m_h - y);
			x += CLIP_W;
		}
		m_caches[xcur][ycur] = new VCache(m_doc, m_pageno, m_scale, x, y, m_w - x, m_h - y);
	}
	protected void vDestroy(VThread thread)
	{
		blocks_destroy(thread);
	}
	protected void vLayout(VThread thread, int x, int y, float scale, boolean clip)
	{
		m_x = x;
		m_y = y;
		m_scale = scale;
		int w = (int)(m_doc.GetPageWidth(m_pageno) * scale);
		int h = (int)(m_doc.GetPageHeight(m_pageno) * scale);
        if( w > (CLIP_W<<2) || h > (CLIP_H<<2)) clip = true;
		if(w != m_w || h != m_h)
		{
			blocks_destroy(thread);
			m_w = w;
			m_h = h;
			if(clip)
				blocks_create();
			else
			{
				m_caches = new VCache[1][1];
				m_caches[0][0] = new VCache(m_doc, m_pageno, scale, 0, 0, w, h);
			}
		}
	}
	protected void vEndPage(VThread thread)
	{
		if(m_caches == null) return;
		int xcur = 0;
		int ycur = 0;
		int xcnt = m_caches.length;
		int ycnt = m_caches[0].length;
		for(ycur = 0; ycur < ycnt; ycur++)
		{
			for(xcur = 0; xcur < xcnt; xcur++)
			{
				VCache cache = m_caches[xcur][ycur];
				if(cache.vIsRender()) m_caches[xcur][ycur] = cache.clone();
				thread.end_render(cache);
			}
		}
		if(m_zoom_bmp != null)
		{
			m_zoom_bmp.recycle();
			m_zoom_bmp = null;
		}
	}
	protected boolean vFinished()
	{
		if(m_caches == null)
        {
            return false;
        }
		int xcnt = m_caches.length;
		int ycnt = m_caches[0].length;
		for( int ycur = 0; ycur < ycnt; ycur++ )
		{
			for( int xcur = 0; xcur < xcnt; xcur++ )
			{
				if( !m_caches[xcur][ycur].vFinished() )
                {
                    return false;
                }
			}
		}
		return true;
	}
    private int m_x0;
    private int m_y0;
    private int m_x1;
    private int m_y1;
    private int m_xb0;
    private int m_yb0;
    private final void get_xyb0(int vx, int vy, int vw, int vh, int xcnt, int ycnt)
    {
        m_x0 = m_x - vx;
        m_y0 = m_y - vy;
        m_x1 = m_x0 + m_w + CLIP_W;
        m_y1 = m_y0 + m_h + CLIP_H;
        if( m_x1 > vw ) m_x1 = vw;
        if( m_y1 > vh ) m_y1 = vh;
        m_xb0 = 0;
        while( m_xb0 < xcnt && m_x0 <= -m_caches[m_xb0][0].vGetW() )
        {
            m_x0 += m_caches[m_xb0][0].vGetW();
            m_xb0++;
        }
        m_yb0 = 0;
        while( m_yb0 < ycnt && m_y0 <= -m_caches[0][m_yb0].vGetH() )
        {
            m_y0 += m_caches[0][m_yb0].vGetH();
            m_yb0++;
        }
    }
    private final void end_render_xb(VThread thread, int xb0, int xb1, int yb)
    {
        while(xb0 < xb1)
        {
            VCache cache = m_caches[xb0][yb];
            if(cache.vIsRender()) m_caches[xb0][yb] = cache.clone();
            thread.end_render(cache);
            xb0++;
        }
    }
    private final void end_render_yb(VThread thread, int yb0, int yb1, int xcnt)
    {
        while(yb0 < yb1)
        {
            end_render_xb(thread, 0, xcnt, yb0);
            yb0++;
        }
    }
	protected void vRenderAsync(VThread thread, int vx, int vy, int vw, int vh)
	{
		if(m_caches == null) return;
		int bmpw = vw + CLIP_W;
		int bmph = vh + CLIP_H;
		int xcnt = m_caches.length;
		int ycnt = m_caches[0].length;
        get_xyb0(vx, vy, vw, vh, xcnt, ycnt);
		int xb0 = m_xb0;
		int yb0 = m_yb0;
        int x0 = m_x0;
        int y0 = m_y0;
		int ycur = yb0;
		int xcur;
        end_render_yb(thread, 0, yb0, xcnt);
		for(int y = y0; y < m_y1 && ycur < ycnt; ycur++)
		{
            end_render_xb(thread, 0, xb0, ycur);
            xcur = xb0;
			for(int x = x0; x < m_x1 && xcur < xcnt; xcur++)
			{
                VCache vc = m_caches[xcur][ycur];
				thread.start_render(vc);
				x += vc.vGetW();
			}
            end_render_xb(thread, xcur, xcnt, ycur);
			y += m_caches[0][ycur].vGetH();
		}
        end_render_yb(thread, ycur, ycnt, xcnt);
	}
	protected void vRenderSync(VThread thread, int vx, int vy, int vw, int vh)
	{
		if(m_caches == null) return;
		int bmpw = vw + CLIP_W;
		int bmph = vh + CLIP_H;
		int xcnt = m_caches.length;
		int ycnt = m_caches[0].length;
        get_xyb0(vx, vy, vw, vh, xcnt, ycnt);
        int xb0 = m_xb0;
        int yb0 = m_yb0;
        int x0 = m_x0;
        int y0 = m_y0;
		int ycur = yb0;
		int xcur;
        end_render_yb(thread, 0, yb0, xcnt);
		for(int y = y0; y < m_y1 && ycur < ycnt; ycur++)
		{
            end_render_xb(thread, 0, xb0, ycur);
            xcur = xb0;
			for(int x = x0; x < m_x1 && xcur < xcnt; xcur++)
			{
                VCache vc = m_caches[xcur][ycur];
				if(vc.vIsRender()) m_caches[xcur][ycur] = vc.clone();
				thread.end_render(vc);
                vc = m_caches[xcur][ycur];
				vc.vRender();
				x += vc.vGetW();
			}
            end_render_xb(thread, xcur, xcnt, ycur);
			y += m_caches[0][ycur].vGetH();
		}
        end_render_yb(thread, ycur, ycnt, xcnt);
	}
    protected void vCacheStart0(VThread thread, float pdfx, float pdfy)
    {
        int blk0 = 0;
        int blk = m_cache.blk_get(pdfx, pdfy);
        int blk_cnt = m_cache.blk_get_count();
        while(blk0 < blk)
        {
            thread.end_render_cache(m_cache, blk0);
            blk0++;
        }
        while(blk < blk_cnt)
        {
            thread.start_render_cache(m_cache, blk);
            blk++;
        }
    }
    protected  void vCacheStart1(VThread thread)
    {
        int blk = 0;
        int blk_cnt = m_cache.blk_get_count();
        while(blk < blk_cnt)
        {
            thread.start_render_cache(m_cache, blk);
            blk++;
        }
    }
    protected void vCacheStart2(VThread thread, float pdfx, float pdfy)
    {
        int blk0 = 0;
        int blk = m_cache.blk_get(pdfx, pdfy);
        int blk_cnt = m_cache.blk_get_count();
        while(blk0 <= blk)
        {
            thread.start_render_cache(m_cache, blk0);
            blk0++;
        }
        while(blk0 < blk_cnt)
        {
            thread.end_render_cache(m_cache, blk0);
            blk0++;
        }
    }
    protected  void vCacheEnd(VThread thread)
    {
        int blk = 0;
        int blk_cnt = m_cache.blk_get_count();
        while(blk < blk_cnt)
        {
            thread.end_render_cache(m_cache, blk);
            blk++;
        }
    }
	protected VPageRenderResult vDraw(VThread thread, BMP bmp, int vx, int vy)
	{
		if(m_caches == null) return null;
        VPageRenderResult ret = new VPageRenderResult(m_caches.length, m_caches[0].length);
        boolean empty = true;
		ret.bmpw = bmp.GetWidth();
		ret.bmph = bmp.GetHeight();
		ret.xcnt = m_caches.length;
		ret.ycnt = m_caches[0].length;
        get_xyb0(vx, vy, ret.bmpw, ret.bmph, ret.xcnt, ret.ycnt);
        ret.xb0 = m_xb0;
        ret.yb0 = m_yb0;
        ret.x0 = m_x0;
        ret.y0 = m_y0;
		int ycur = ret.yb0;
		int xcur;
        end_render_yb(thread, 0, ret.yb0, ret.xcnt);
		for(int y = ret.y0; y < m_y1 && ycur < ret.ycnt; ycur++)
		{
            end_render_xb(thread, 0, ret.xb0, ycur);
            xcur = ret.xb0;
			for(int x = ret.x0; x < m_x1 && xcur < ret.xcnt; xcur++)
			{
                VCache vc = m_caches[xcur][ycur];
				thread.start_render(vc);
                if(vc.vRenderFinished())
                {
                    vc.vDraw(bmp, x, y);
                    ret.m_flags[xcur][ycur] = true;
                }
                else empty = false;
				x += vc.vGetW();
			}
            end_render_xb(thread, xcur, ret.xcnt, ycur);
			y += m_caches[0][ycur].vGetH();
		}
        end_render_yb(thread, ycur, ret.ycnt, ret.xcnt);
        if(!empty) return ret;
        else return null;
	}
    protected boolean vDrawStep1(Canvas canvas, VPageRenderResult result)
    {
        if(m_caches == null || result == null) return false;
        boolean has_empty = false;
        int ycur = result.yb0;
        int xcur;
        float mul = 1 / m_scale;
        for(int y = result.y0; y < m_y1 && ycur < result.ycnt; ycur++)
        {
            xcur = result.xb0;
            for(int x = result.x0; x < m_x1 && xcur < result.xcnt; xcur++)
            {
                VCache vc = m_caches[xcur][ycur];
                if(!result.m_flags[xcur][ycur])
                {
                    VCache cache = m_caches[xcur][ycur];
                    float src_x1 = cache.vGetX() * mul;
                    float src_y1 = m_doc.GetPageHeight(m_pageno) - cache.vGetY() * mul;
                    float src_x2 = (cache.vGetX() + cache.vGetW()) * mul;
                    float src_y2 = m_doc.GetPageHeight(m_pageno) - (cache.vGetY() + cache.vGetH()) * mul;
                    result.m_flags[xcur][ycur] = m_cache.blk_draw(canvas, src_x1, src_y1, src_x2, src_y2, x, y, m_scale);
                }
                if(!result.m_flags[xcur][ycur])
                    has_empty = true;
                x += vc.vGetW();
            }
            y += m_caches[0][ycur].vGetH();
        }
        return has_empty;
    }
    protected void vDrawStep2(BMP bmp, VPageRenderResult result)
    {
        if(m_caches == null || result == null) return;
        int ycur = result.yb0;
        int xcur;
        for(int y = result.y0; y < m_y1 && ycur < result.ycnt; ycur++)
        {
            xcur = result.xb0;
            for(int x = result.x0; x < m_x1 && xcur < result.xcnt; xcur++)
            {
                VCache vc = m_caches[xcur][ycur];
                if(!result.m_flags[xcur][ycur])
                    vc.vDraw(bmp, x, y);
                x += vc.vGetW();
            }
            y += m_caches[0][ycur].vGetH();
        }
    }
	protected void vDraw(Canvas canvas, int vx, int vy)
	{
        if(!m_cache.blk_draw(canvas, 0, m_doc.GetPageHeight(m_pageno), m_doc.GetPageWidth(m_pageno), 0, m_x - vx, m_y - vy, m_scale))
        {
            Rect rect = new Rect();
            rect.left = m_x - vx;
            rect.top = m_y - vy;
            rect.right = rect.left + m_w;
            rect.bottom = rect.top + m_h;
            if (m_zoom_bmp != null)
                canvas.drawBitmap(m_zoom_bmp, null, rect, null);
            else
            {
                Paint paint = new Paint();
                paint.setStyle(Paint.Style.FILL);
                paint.setColor(0xFFFFFFFF);
                canvas.drawRect(rect, paint);
            }
        }
	}
	protected void vZoomStart(Config bmp_format)
	{
        if(m_cache.blk_rendered()) return;
		long total = (long)m_w * (long)m_h;
		int w = m_w;
		int h = m_h;
		int bits = 0;
		while( total > (1<<20) )
		{
			total >>= 2;
			w >>= 1;
			h >>= 1;
			bits++;
		}
		while( m_zoom_bmp == null )
		{
			try
			{
				m_zoom_bmp = Bitmap.createBitmap(w, h, bmp_format);
			}
			catch(Exception e)
			{
				total >>= 2;
				w >>= 1;
				h >>= 1;
				bits++;
			}
			if( bits > 8 )
                return;
		}
		BMP bmp = new BMP();
		bmp.Create(m_zoom_bmp);
		int xcnt = m_caches.length;
		int ycnt = m_caches[0].length;
		int ycur = 0;
		int xcur;
		if( bits == 0 )//just copy
		{
			for(int y = 0; ycur < ycnt; ycur++)
			{
				xcur = 0;
				for(int x = 0; xcur < xcnt; xcur++)
				{
					m_caches[xcur][ycur].vDraw(bmp, x, y );
					x += m_caches[xcur][ycur].vGetW();
				}
				y += m_caches[0][ycur].vGetH();
			}
		}
		else//need scale
		{
			for(int y = 0; ycur < ycnt; ycur++)
			{
				xcur = 0;
				for(int x = 0; xcur < xcnt; xcur++)
				{
					VCache cache = m_caches[xcur][ycur];
					cache.vDraw(bmp, x>>bits, y>>bits, cache.vGetW()>>bits, cache.vGetH()>>bits );
					x += m_caches[xcur][ycur].vGetW();
				}
				y += m_caches[0][ycur].vGetH();
			}
		}
		bmp.Free(m_zoom_bmp);
	}
	protected void vZoomConfirmed(VThread thread, int vx, int vy, int vw, int vh)
	{
		int xcnt = m_caches.length;
		int ycnt = m_caches[0].length;
        get_xyb0(vx, vy, vw, vh, xcnt, ycnt);
        int xb0 = m_xb0;
        int yb0 = m_yb0;
        int x0 = m_x0;
        int y0 = m_y0;
		int ycur = yb0;
		int xcur;
        end_render_yb(thread, 0, yb0, xcnt);
		for(int y = y0; y < m_y1 && ycur < ycnt; ycur++)
		{
            end_render_xb(thread, 0, xb0, ycur);
            xcur = xb0;
			for(int x = x0; x < m_x1 && xcur < xcnt; xcur++)
			{
				thread.start_render(m_caches[xcur][ycur]);
				x += m_caches[xcur][ycur].vGetW();
			}
            end_render_xb(thread, xcur, xcnt, ycur);
			y += m_caches[0][ycur].vGetH();
		}
        end_render_yb(thread, ycur, ycnt, xcnt);
	}
	protected void vZoomEnd()
	{
		if(m_zoom_bmp != null)
			m_zoom_bmp.recycle();
		m_zoom_bmp = null;
	}
	/**
	 * map x position in view to PDF coordinate
	 * @param x x position in view
	 * @param scrollx x scroll position
	 * @return
	 */
	public final float ToPDFX( float x, float scrollx )
	{
		float dibx = scrollx + x - m_x;
		return dibx / m_scale;
	}
	/**
	 * map y position in view to PDF coordinate
	 * @param y y position in view
	 * @param scrolly y scroll position
	 * @return
	 */
	public final float ToPDFY( float y, float scrolly )
	{
		float diby = scrolly + y - m_y;
		return (m_h - diby) / m_scale;
	}
	/**
	 * map x to DIB coordinate
	 * @param x x position in PDF coordinate
	 * @return
	 */
	public final float ToDIBX( float x )
	{
		return x * m_scale;
	}
	/**
	 * map y to DIB coordinate
	 * @param y y position in PDF coordinate
	 * @return
	 */
	public final float ToDIBY( float y )
	{
		return (m_doc.GetPageHeight(m_pageno) - y) * m_scale;
	}
	public final float ToPDFSize( float val )
	{
		return val / m_scale;
	}
	/**
	 * create an Inverted Matrix maps screen coordinate to PDF coordinate.
	 * @param scrollx current x for PDFView
	 * @param scrolly current y for PDFView
	 * @return
	 */
	public final Matrix CreateInvertMatrix( float scrollx, float scrolly )
	{
		return new Matrix( 1/m_scale, -1/m_scale, (scrollx - m_x)/m_scale, (m_y + m_h - scrolly)/m_scale );
	}

}
