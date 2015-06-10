package org.fiteagle.core.federationManager.dm;


import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.jena.atlas.logging.Log;


public class ControlFilter implements Filter {

    @Override
    public void destroy() {
        // TODO Auto-generated method stub
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        String uri = req.getRequestURI();
        String path = uri.substring(req.getContextPath().length());
    
        if (path.startsWith("/ontology") && req.getMethod().equals("POST") && req.getHeader("Token").equalsIgnoreCase("test123"))  {
        	request.getRequestDispatcher("/ontology").forward(request, response);
        }else{
        request.getRequestDispatcher("/failedAuth").forward(request, response);
    	Log.warn("Config-Filter", "Someone tried to push an Ontology-File with incorrect token");
        }
        	
    }
        @Override
        public void init (FilterConfig arg0)throws ServletException {
            // TODO Auto-generated method stub

        }

    }
