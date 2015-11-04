package com.radaee.view;

import com.radaee.pdf.BMP;
import com.radaee.pdf.DIB;
import com.radaee.pdf.Document;
import com.radaee.pdf.Matrix;
import com.radaee.pdf.Page;

public class VCache
{
	private Document m_doc;
	private Page m_page;
	private int m_pageno;
	private float m_scale;
	private int m_x;
	private int m_y;
	private int m_w;
	private int m_h;
	private DIB m_dib;
	private int m_status;
	private boolean m_render;
	protected VCache(Document doc, int pageno, float scale, int x, int y, int w, int h)
	{
		m_doc = doc;
		m_pageno = pageno;
		m_page = null;
		m_scale = scale;
		m_x = x;
		m_y = y;
		m_w = w;
		m_h = h;
		m_dib = null;
		m_status = 0;
		m_render = false;
	}
    final protected boolean vIsRender()
	{
		return (m_render || m_status != 0 || m_dib != null);
	}
    final protected VCache clone()
	{
		return new VCache(m_doc, m_pageno, m_scale, m_x, m_y, m_w, m_h);
	}
    final protected boolean vStart()
	{
		if(!m_render)
		{
			m_render = true;
			m_status = 0;
			return true;
		}
		else
			return false;
	}
    final protected boolean vEnd()
	{
		if(m_render)
		{
			m_render = false;
			if(m_status <= 0) m_status = -1;
			if(m_page != null) m_page.RenderCancel();
			return true;
		}
		else
			return false;
	}
    protected final boolean vRenderFinished()
    {
        return (m_render && m_status > 0);
    }
	protected final boolean vFinished()
	{
		return (!m_render && m_status == 0) || (m_render && m_status > 0);
	}
	protected void vRender()
	{
		DIB dib = new DIB();
		dib.CreateOrResize(m_w, m_h);
		if(m_status < 0)
		{
			dib.Free();
			return;
		}
		m_page = m_doc.GetPage(m_pageno);
		m_page.RenderPrePare(dib);
		m_dib = dib;
		if(m_status < 0) return;
		Matrix mat = new Matrix(m_scale, -m_scale, -m_x, m_doc.GetPageHeight(m_pageno) * m_scale - m_y);
		m_page.Render(dib, mat);
		mat.Destroy();
		if(m_status >= 0) m_status = 1;
	}
	protected void vRenderThumb()
	{
		DIB dib = new DIB();
		dib.CreateOrResize(m_w, m_h);
		if(m_status < 0)
		{
			dib.Free();
			return;
		}
		m_page = m_doc.GetPage(m_pageno);
		m_page.RenderPrePare(dib);
		m_dib = dib;
		if(m_status < 0) return;
		Matrix mat = new Matrix(m_scale, -m_scale, -m_x, m_doc.GetPageHeight(m_pageno) * m_scale - m_y);
		m_page.Render(dib, mat);
		mat.Destroy();
		if(m_status >= 0) m_status = 1;
	}
	protected void vDestroy()
	{
		if(m_dib != null) m_dib.Free();
		if(m_page != null) m_page.Close();
		m_status = 0;
		m_dib = null;
		m_page = null;
		m_render = false;
	}
    final protected void vDraw(BMP bmp, int x, int y)
	{
		if( m_render && m_dib != null )
			m_dib.DrawToBmp(bmp, x, y);
		else
			bmp.DrawRect(0xFFFFFFFF, x, y, m_w, m_h, 1);
	}
    final protected void vDraw(BMP bmp, int x, int y, int w, int h)
	{
		if( m_render && m_dib != null )
			m_dib.DrawToBmp2(bmp, x, y, w, h);
		else
			bmp.DrawRect(0xFFFFFFFF, x, y, w, h, 1);
	}
	protected final int vGetW(){return m_w;}
	protected final int vGetH(){return m_h;}
    protected final int vGetX(){return m_x;}
    protected final int vGetY(){return m_y;}
	protected final int vGetPageNO(){return m_pageno;};
    @Override
    protected void finalize() throws Throwable
    {
        vDestroy();
        super.finalize();
    }
}
