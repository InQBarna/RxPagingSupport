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

/**
 * @author David García <david.garcia@inqbarna.com>
 */
public class PageRequest {

    static PageRequest createFromPageAndSize(Type type, int page, int pageSize) {
        PageRequest request = new PageRequest();
        // first page is 0
        if (page < 0) {
            throw new IllegalArgumentException("Page cannot be smaller than 0");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be a positive number");
        }

        // for this to be true, pageSize must be same for all pages!
        request.type = type;
        request.offset = page * pageSize;
        request.size = pageSize;
        return request;
    }

    static PageRequest createFromOffsetEnd(Type type, int offset, int end) {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must be >= 0");
        }

        if (end <= offset) {
            throw new IllegalArgumentException("End must be greater than offset");
        }

        PageRequest request = new PageRequest();
        request.type = type;
        request.offset = offset;
        request.size = end - offset + 1;
        return request;
    }

    public int getPage() {
        return offset / size;
    }

    public enum Type {
        /** Full request, return first any available disk info, then retrieve from network for updates */
        Network,
        /** Request page only on local storage */
        Disk
    }

    private Type type;
    private int offset;
    private int size;



    private PageRequest() {
    }

    public Type getType() {
        return type;
    }

    public int getOffset() {
        return offset;
    }

    public int getSize() {
        return size;
    }

    public int getEnd() {
        return offset + size - 1;
    }

    @Override
    public String toString() {
        return "PageRequest{" +
                "type=" + type +
                ", offset=" + offset +
                ", size=" + size +
                '}';
    }
}
