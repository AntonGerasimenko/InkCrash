package com.radaee.view;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.radaee.pdf.Document;

/**
 * Created by radaee on 2015/5/14.
 */
public class PDFViewPager extends ViewPager
{
    private PDFPageView m_pages[] = null;
    private VThread m_thread;
    private Handler m_hand_ui = new Handler(Looper.myLooper())
    {
        public void handleMessage(Message msg)
        {
            switch( msg.what )//render finished.
            {
                case 0:
                    //if(m_listener != null) m_listener.OnPageRendered(((VCache)msg.obj).vGetPageNO());
                {
                    int pageno = ((VCache) msg.obj).vGetPageNO();
                    if(m_pages[pageno].vIsRenderFinish())
                        m_pages[pageno].vRenderFinish();
                }
                    break;
                case 1://find operation returned.
                    if( msg.arg1 == 1 )//succeeded
                    {
                        //vFindGoto();
                        //if( m_listener != null )
                        //    m_listener.OnFound( true );
                    }
                    else
                    {
                        //if( m_listener != null )
                        //    m_listener.OnFound( false );
                    }
                    break;
                case 100://timer
                    m_pages[PDFViewPager.this.getCurrentItem()].invalidate();
                    //if(m_listener != null) m_listener.OnTimer();
                    break;
            }
            super.handleMessage(msg);
        }
    };
    private class PDFPageAdapter extends PagerAdapter
    {
        public PDFPageAdapter(Context ctx, Document doc)
        {
            m_thread = new VThread(m_hand_ui);
            m_thread.start();
            int cnt = doc.GetPageCount();
            m_pages = new PDFPageView[cnt];
            int cur;
            for(cur = 0; cur < cnt; cur++)
            {
                m_pages[cur] = new PDFPageView(ctx);
                m_pages[cur].vOpen(m_thread, doc, cur, m_fit_type);
            }
        }
        @Override
        public java.lang.Object instantiateItem(android.view.ViewGroup container, int position)
        {
            container.addView(m_pages[position]);
            return m_pages[position];
        }
        @Override
        public int getCount()
        {
            return m_pages.length;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object)
        {
            m_pages[position].vFreeCache();
            container.removeView(m_pages[position]);
        }
        @Override
        public boolean isViewFromObject(View view, Object o)
        {
            return view == o;
        }
        @Override
        public CharSequence getPageTitle(int position)
        {
            return "Page:" + position;
        }
    }
    private PDFPageAdapter m_adt;
    public PDFViewPager(Context context)
    {
        super(context);
    }
    public PDFViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    private int m_fit_type;

    /**
     *
     * @param doc
     * @param fit_type page fit mode: 0- fit screen. 1- fit width. 2- fit height.
     */
    public void PDFOpen(Document doc, int fit_type)
    {
        VPage.SetConfigure(getContext());
        m_fit_type = fit_type;
        m_adt = new PDFPageAdapter(getContext(), doc);
        this.setAdapter(m_adt);
        setCurrentItem(0);
    }
    public void PDFClose()
    {
        if(m_pages != null) {
            int cur = 0;
            int cnt = m_pages.length;
            for(cur = 0; cur < cnt; cur++)
            {
                m_pages[cur].vClose();
            }
        }
        if(m_thread != null) {
            m_thread.destroy();
            m_thread = null;
        }
    }
    @Override
    protected void finalize() throws Throwable
    {
        PDFClose();
        super.finalize();
    }
}
