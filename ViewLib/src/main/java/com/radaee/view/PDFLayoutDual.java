package com.radaee.view;

import android.content.Context;

public class PDFLayoutDual extends PDFLayout
{
	private boolean m_vert_dual[];
	private boolean m_horz_dual[];
	private boolean m_rtol = false;
    private boolean m_page_align_top = true;
	private class PDFCell
	{
		int left;
		int right;
		int page_left;
		int page_right;
	}
	private PDFCell m_cells[];
	public PDFLayoutDual(Context context)
	{
		super(context);
	}
	@Override
	public boolean vFling(int holdx, int holdy, float dx, float dy, float vx, float vy)
	{
		float ddx = (vx<0)?-vx:vx;
		float ddy = (vy<0)?-vy:vy;
		if( ddx < ddy ) return false;
		if( dx < (m_w>>2) && dx > -(m_w>>2) ) return false;
		if( ddx < (m_w>>1) && ddx > -(m_w>>1) ) return false;
		int x = vGetX();
		int y = vGetY();
		int ccur = 0;
		while( ccur < m_cells.length )
		{
			PDFCell cell = m_cells[ccur];
			if( holdx >= cell.left && holdx < cell.right )
			{
                vScrollAbort();
				if( m_rtol )
				{
					if( dx > 0 )
					{
						if( ccur < m_cells.length - 1 )
						{
							int endx = m_cells[ccur+1].right - m_w;
							m_scroller.startScroll(x, y, endx - x, 0);
						}
						else
							m_scroller.startScroll(x, y, -x, 0);
					}
					else
					{
						if( ccur > 0 )
						{
							int endx = cell.right;
							m_scroller.startScroll(x, y, endx - x, 0);
						}
						else
							m_scroller.startScroll(x, y, m_cells[ccur].right - m_w, 0);
					}
				}
				else
				{
					if( dx > 0 )
					{
						if( ccur > 0 )
						{
							int endx = m_cells[ccur-1].right - m_w;
							m_scroller.startScroll(x, y, endx - x, 0);
						}
						else
							m_scroller.startScroll(x, y, -x, 0);
					}
					else
					{
						if( ccur < m_cells.length - 1 )
						{
							int endx = cell.right;
							m_scroller.startScroll(x, y, endx - x, 0);
						}
						else
							m_scroller.startScroll(x, y, m_cells[ccur].right - m_w, 0);
					}
				}
				return true;
			}
			ccur++;
		}
		return false;
	}
	@Override
	public void vLayout()
	{
		if( m_doc == null || m_w <= m_page_gap || m_h <= m_page_gap ) return;
		int pcur = 0;
		int pcnt = m_doc.GetPageCount();
		int ccur = 0;
		int ccnt = 0;
		float max_w = 0;
		float max_h = 0;
		if( m_pages == null ) m_pages = new VPage[pcnt];
		if( m_h > m_w )//vertical
		{
			while( pcur < pcnt )
			{
				if( m_vert_dual != null && ccnt < m_vert_dual.length && m_vert_dual[ccnt] && pcur < pcnt - 1 )
				{
					float w = m_doc.GetPageWidth(pcur) + m_doc.GetPageWidth(pcur + 1);
					if( max_w < w ) max_w = w;
					float h = m_doc.GetPageHeight(pcur);
					if( max_h < h ) max_h = h;
					h = m_doc.GetPageHeight(pcur + 1);
					if( max_h < h ) max_h = h;
					pcur += 2;
				}
				else
				{
					float w = m_doc.GetPageWidth(pcur);
					if( max_w < w ) max_w = w;
					float h = m_doc.GetPageHeight(pcur);
					if( max_h < h ) max_h = h;
					pcur++;
				}
				ccnt++;
			}
            m_scale_min = ((float)(m_w - m_page_gap)) / max_w;
            float scale = ((float)(m_h - m_page_gap)) / max_h;
            if( m_scale_min > scale ) m_scale_min = scale;
            m_scale_max = m_scale_min * m_zoom_level;
			if( m_scale < m_scale_min ) m_scale = m_scale_min;
			if( m_scale > m_scale_max ) m_scale = m_scale_max;
			boolean clip = m_scale / m_scale_min > m_zoom_level_clip;
			m_th = (int)(max_h * m_scale) + m_page_gap;
			if( m_th < m_h ) m_th = m_h;
			m_cells = new PDFCell[ccnt];
			pcur = 0;
			ccur = 0;
			int left = 0;
			while( ccur < ccnt )
			{
				PDFCell cell = new PDFCell();
				int w = 0;
				int cw = 0;
				if( m_vert_dual != null && ccur < m_vert_dual.length && m_vert_dual[ccur] && pcur < pcnt - 1 )
				{
					w = (int)( (m_doc.GetPageWidth(pcur) + m_doc.GetPageWidth(pcur + 1)) * m_scale );
					if( w + m_page_gap < m_w ) cw = m_w;
					else cw = w + m_page_gap;
					cell.page_left = pcur;
					cell.page_right = pcur + 1;
					cell.left = left;
					cell.right = left + cw;
					if( m_pages[pcur] == null ) m_pages[pcur] = new VPage(m_doc, pcur, m_bmp_format);
					if( m_pages[pcur+1] == null ) m_pages[pcur+1] = new VPage(m_doc, pcur+1, m_bmp_format);
                    if(m_page_align_top)
                    {
                        m_pages[pcur].vLayout(m_thread, left + (cw - w) / 2, m_page_gap / 2, m_scale, clip);
                        m_pages[pcur + 1].vLayout(m_thread, m_pages[pcur].GetX() + m_pages[pcur].GetWidth(), m_page_gap / 2, m_scale, clip);
                    }
                    else
                    {
                        m_pages[pcur].vLayout(m_thread, left + (cw - w) / 2, (int) (m_th - m_doc.GetPageHeight(pcur) * m_scale) / 2, m_scale, clip);
                        m_pages[pcur + 1].vLayout(m_thread, m_pages[pcur].GetX() + m_pages[pcur].GetWidth(), (int) (m_th - m_doc.GetPageHeight(pcur + 1) * m_scale) / 2, m_scale, clip);
                    }
					pcur += 2;
				}
				else
				{
					w = (int)( m_doc.GetPageWidth(pcur) * m_scale );
					if( w + m_page_gap < m_w ) cw = m_w;
					else cw = w + m_page_gap;
					cell.page_left = pcur;
					cell.page_right = -1;
					cell.left = left;
					cell.right = left + cw;
					if( m_pages[pcur] == null ) m_pages[pcur] = new VPage(m_doc, pcur, m_bmp_format);
                    if(m_page_align_top) {
                        m_pages[pcur].vLayout(m_thread, left + (cw - w) / 2, m_page_gap / 2, m_scale, clip);
                    }
                    else {
                        m_pages[pcur].vLayout(m_thread, left + (cw - w) / 2, (int) (m_th - m_doc.GetPageHeight(pcur) * m_scale) / 2, m_scale, clip);
                    }
					pcur++;
				}
				left += cw;
				m_cells[ccur] = cell;
				ccur++;
			}
			m_tw = left;
		}
		else
		{
			while( pcur < pcnt )
			{
				if( (m_horz_dual == null || ccnt >= m_horz_dual.length || m_horz_dual[ccnt]) && pcur < pcnt - 1 )
				{
					float w = m_doc.GetPageWidth(pcur) + m_doc.GetPageWidth(pcur + 1);
					if( max_w < w ) max_w = w;
					float h = m_doc.GetPageHeight(pcur);
					if( max_h < h ) max_h = h;
					h = m_doc.GetPageHeight(pcur + 1);
					if( max_h < h ) max_h = h;
					pcur += 2;
				}
				else
				{
					float w = m_doc.GetPageWidth(pcur);
					if( max_w < w ) max_w = w;
					float h = m_doc.GetPageHeight(pcur);
					if( max_h < h ) max_h = h;
					pcur++;
				}
				ccnt++;
			}
			m_scale_min = ((float)(m_w - m_page_gap)) / max_w;
			float scale = ((float)(m_h - m_page_gap)) / max_h;
			if( m_scale_min > scale ) m_scale_min = scale;
			m_scale_max = m_scale_min * m_zoom_level;
			if( m_scale < m_scale_min ) m_scale = m_scale_min;
			if( m_scale > m_scale_max ) m_scale = m_scale_max;
			boolean clip = m_scale / m_scale_min > m_zoom_level_clip;
			m_th = (int)(max_h * m_scale) + m_page_gap;
			if( m_th < m_h ) m_th = m_h;
			m_cells = new PDFCell[ccnt];
			pcur = 0;
			ccur = 0;
			int left = 0;
			while( ccur < ccnt )
			{
				PDFCell cell = new PDFCell();
				int w = 0;
				int cw = 0;
				if( (m_horz_dual == null || ccur >= m_horz_dual.length || m_horz_dual[ccur]) && pcur < pcnt - 1 )
				{
					w = (int)( (m_doc.GetPageWidth(pcur) + m_doc.GetPageWidth(pcur + 1)) * m_scale );
					if( w + m_page_gap < m_w ) cw = m_w;
					else cw = w + m_page_gap;
					cell.page_left = pcur;
					cell.page_right = pcur + 1;
					cell.left = left;
					cell.right = left + cw;
					if( m_pages[pcur] == null ) m_pages[pcur] = new VPage(m_doc, pcur, m_bmp_format);
					if( m_pages[pcur+1] == null ) m_pages[pcur+1] = new VPage(m_doc, pcur+1, m_bmp_format);
                    if(m_page_align_top)
                    {
                        m_pages[pcur].vLayout(m_thread, left + (cw - w) / 2, m_page_gap / 2, m_scale, clip);
                        m_pages[pcur + 1].vLayout(m_thread, m_pages[pcur].GetX() + m_pages[pcur].GetWidth(), m_page_gap / 2, m_scale, clip);
                    }
                    else {
                        m_pages[pcur].vLayout(m_thread, left + (cw - w) / 2, (int) (m_th - m_doc.GetPageHeight(pcur) * m_scale) / 2, m_scale, clip);
                        m_pages[pcur + 1].vLayout(m_thread, m_pages[pcur].GetX() + m_pages[pcur].GetWidth(), (int) (m_th - m_doc.GetPageHeight(pcur + 1) * m_scale) / 2, m_scale, clip);
                    }
					pcur += 2;
				}
				else
				{
					w = (int)( m_doc.GetPageWidth(pcur) * m_scale );
					if( w + m_page_gap < m_w ) cw = m_w;
					else cw = w + m_page_gap;
					cell.page_left = pcur;
					cell.page_right = -1;
					cell.left = left;
					cell.right = left + cw;
					if( m_pages[pcur] == null ) m_pages[pcur] = new VPage(m_doc, pcur, m_bmp_format);
                    if(m_page_align_top)
                    {
                        m_pages[pcur].vLayout(m_thread, left + (cw - w) / 2, m_page_gap / 2, m_scale, clip);
                    }
                    else {
                        m_pages[pcur].vLayout(m_thread, left + (cw - w) / 2, (int) (m_th - m_doc.GetPageHeight(pcur) * m_scale) / 2, m_scale, clip);
                    }
					pcur++;
				}
				left += cw;
				m_cells[ccur] = cell;
				ccur++;
			}
			m_tw = left;
		}
		if( m_rtol )
		{
			ccur = 0;
			pcur = 0;
			while( ccur < ccnt )
			{
				PDFCell cell = m_cells[ccur];
				int tmp = cell.left;
				cell.left = m_tw - cell.right;
				cell.right = m_tw - tmp;
				if( cell.page_right >= 0 )
				{
					tmp = cell.page_left;
					cell.page_left = cell.page_right;
					cell.page_right = tmp;
				}
				ccur++;
			}
			while( pcur < pcnt )
			{
				VPage vpage = m_pages[pcur];
				vpage.SetX(m_tw - (vpage.GetX() + vpage.GetWidth()));
				pcur++;
			}
		}
	}

	@Override
	public int vGetPage(int vx, int vy)
	{
		if( m_pages == null || m_pages.length <= 0  || m_cells == null) return -1;
		int left = 0;
		int right = m_cells.length - 1;
		vx += vGetX();
		if( !m_rtol )//ltor
		{
			while( left <= right )
			{
				int mid = (left + right)>>1;
				PDFCell pg1 = m_cells[mid];
				if( vx < pg1.left )
				{
					right = mid - 1;
				}
				else if( vx > pg1.right )
				{
					left = mid + 1;
				}
				else
				{
					VPage vpage = m_pages[pg1.page_left];
					if(pg1.page_right >= 0 && vx > vpage.GetX() + vpage.GetWidth() )
						return pg1.page_right;
					else
						return pg1.page_left;
				}
			}
		}
		else//rtol
		{
			while( left <= right )
			{
				int mid = (left + right)>>1;
				PDFCell pg1 = m_cells[mid];
				if( vx < pg1.left )
				{
					left = mid + 1;
				}
				else if( vx > pg1.right )
				{
					right = mid - 1;
				}
				else
				{
					VPage vpage = m_pages[pg1.page_left];
					if(pg1.page_right >= 0 && vx > vpage.GetX() + vpage.GetWidth() )
						return pg1.page_right;
					else
						return pg1.page_left;
				}
			}
		}
		if( right < 0 )
		{
			return 0;
		}
		else
		{
			return m_pages.length - 1;
		}
	}
	@Override
	public void vMoveEnd()
	{
		int ccur = 0;
		int x = vGetX();
		int y = vGetY();
		if( m_rtol )
		{
			while( ccur < m_cells.length )
			{
				PDFCell cell = m_cells[ccur];
				if( x >= cell.left )
				{
			    	m_scroller.abortAnimation();
					m_scroller.forceFinished(true);
					if( x <= cell.right - m_w )
					{
					}
					else if( cell.right - x > m_w/2 )
					{
						m_scroller.startScroll(x, y, cell.right - x - m_w, 0);
					}
					else if( ccur < m_cells.length - 1 )
					{
						m_scroller.startScroll(x, y, cell.right - x, 0);
					}
					else
					{
						m_scroller.startScroll(x, y, cell.right - x - m_w, 0);
					}
					break;
				}
				ccur++;
			}
		}
		else
		{
			while( ccur < m_cells.length )
			{
				PDFCell cell = m_cells[ccur];
				if( x < cell.right )
				{
					m_scroller.abortAnimation();
					m_scroller.forceFinished(true);
					if( x <= cell.right - m_w )
					{
					}
					else if( cell.right - x > m_w/2 )
					{
						m_scroller.startScroll(x, y, cell.right - x - m_w, 0);
					}
					else if( ccur < m_cells.length - 1 )
					{
						m_scroller.startScroll(x, y, cell.right - x, 0);
					}
					else
					{
						m_scroller.startScroll(x, y, cell.right - x - m_w, 0);
					}
					break;
				}
				ccur++;
			}
		}
	}
	@Override
	public void vGotoPage( int pageno )
	{
		if( m_pages == null || m_doc == null || m_w <= 0 || m_h <= 0 ) return;
		vScrollAbort();
		int ccur = 0;
		while( ccur < m_cells.length )
		{
			PDFCell cell = m_cells[ccur];
			if( pageno == cell.page_left || pageno == cell.page_right )
			{
				int left = m_cells[ccur].left;
				int w = m_cells[ccur].right - left;
				int x = left + (w - m_w)/2;
				m_scroller.setFinalX(x);
				break;
			}
			ccur++;
		}
	}
	public void vSetLayoutPara( boolean verts[], boolean horzs[], boolean rtol, boolean pages_align_top )
	{
		m_vert_dual = verts;
		m_horz_dual = horzs;
		m_rtol = rtol;
        m_page_align_top = pages_align_top;
		vLayout();
	}
}
