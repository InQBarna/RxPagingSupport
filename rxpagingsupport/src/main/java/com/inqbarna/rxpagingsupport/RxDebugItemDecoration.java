/*                                                                              
 *    Copyright 2015 InQBarna Kenkyuu Jo SL                                     
 *                                                                              
 *    Licensed under the Apache License, Version 2.0 (the "License");           
 *    you may not use this file except in compliance with the License.          
 *    You may obtain a copy of the License at                                   
 *                                                                              
 *        http://www.apache.org/licenses/LICENSE-2.0                            
 *                                                                              
 *    Unless required by applicable law or agreed to in writing, software       
 *    distributed under the License is distributed on an "AS IS" BASIS,         
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  
 *    See the License for the specific language governing permissions and       
 *    limitations under the License.                                            
 *                                                                              
 */
package com.inqbarna.rxpagingsupport;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

/**
 * @author David García <david.garcia@inqbarna.com>
 * @version 1.0 5/11/15
 */
class RxDebugItemDecoration extends RecyclerView.ItemDecoration {
    private TextView    counters;
    private PageManager manager;

    private Rect rect;
    private Rect container;

    private Paint diskPaint;
    private Paint netPaint;
    private Paint memoryPaint;

    private Path gPath;
    private int pathWidth;

    private int maxPathDim;

    public RxDebugItemDecoration(RecyclerView recyclerView, PageManager manager) {
        this(recyclerView, manager, true);
    }

    public RxDebugItemDecoration(RecyclerView recyclerView, PageManager manager, boolean drawCounters) {
        counters = drawCounters ? (TextView) LayoutInflater.from(recyclerView.getContext()).inflate(R.layout.rx_dbg_counters, recyclerView, false) : null;
        rect = new Rect();
        container = new Rect();
        this.manager = manager;
        diskPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        diskPaint.setColor(Color.parseColor("#3618C9"));
        diskPaint.setStyle(Paint.Style.FILL);

        netPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        netPaint.setColor(Color.parseColor("#DB0F3F"));
        netPaint.setStyle(Paint.Style.FILL);

        memoryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        memoryPaint.setColor(Color.parseColor("#10E329"));
        memoryPaint.setStyle(Paint.Style.FILL);

        maxPathDim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, recyclerView.getResources().getDisplayMetrics());
    }

    @Override
    public void onDrawOver(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (null != counters) {
            fillStats();
            parent.getDrawingRect(container);
            GravityCompat.apply(Gravity.CENTER_HORIZONTAL, counters.getMeasuredWidth(), counters.getMeasuredHeight(), container, rect, ViewCompat.LAYOUT_DIRECTION_LOCALE);
            c.save();
            c.translate(rect.left, 0);
            counters.draw(c);
            c.restore();
        }

        final RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        Source source;
        for (int i = 0; i < parent.getChildCount(); i++) {
            final View child = parent.getChildAt(i);
            Path path = getPath(child);
            RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int viewAdapterPosition = params.getViewAdapterPosition();
            if (RecyclerView.NO_POSITION != viewAdapterPosition) {
                source = manager.getSourceOfPos(viewAdapterPosition);
                if (null != source) {
                    c.save();
                    c.translate(layoutManager.getDecoratedRight(child) - pathWidth, layoutManager.getDecoratedTop(child));
                    c.drawPath(path, paintForSource(source));
                    c.restore();
                }
            }
        }

    }

    private Paint paintForSource(Source source) {
        switch (source) {
            case Memory:
                return memoryPaint;
            case Cache:
                return diskPaint;
            case Network:
            default:
                return netPaint;
        }
    }

    private Path getPath(View child) {
        if (this.gPath == null) {
            gPath = new Path();
            pathWidth = Math.min(maxPathDim, child.getMeasuredHeight() / 3);
            gPath.rLineTo(pathWidth, 0);
            gPath.rLineTo(0, pathWidth);
            gPath.close();
        }
        return gPath;
    }

    private void fillStats() {
        int numPages = manager.getNumPages();
        int first = manager.getFirstItemOffset();
        int last = manager.getLastItemOffset();
        counters.setText(counters.getResources().getString(R.string.rx_db_counter_text, numPages, first, last, (last - first + 1)));
        final int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        counters.measure(spec, spec);
        counters.layout(0, 0, counters.getMeasuredWidth(), counters.getMeasuredHeight());
    }
}
