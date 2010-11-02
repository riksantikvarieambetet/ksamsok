package se.raa.ksamsok.manipulator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

public class ManipulatorServlet extends HttpServlet
{
	private static final long serialVersionUID = 213148022059727538L;
	
	private DataSource ds = null;
	static final String DATASOURCE_NAME = "harvestdb";
	private static final Map<Thread, Manipulator> threadMap = new HashMap<Thread, Manipulator>();

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{
		String operation = req.getParameter("operation");
		if("stop".equals(operation)) {
			stopThreads();
		}
		req.setAttribute("manipulators", threadMap.values());
		if(operation != null) {
			boolean proccessRunning = false;
			if(operation.equals("nativeUrl")) {
				for(Map.Entry<Thread, Manipulator> entry : threadMap.entrySet()) {
					if(entry.getValue() instanceof NativeUrlManipulator) {
						if(entry.getValue().isRunning()) {
							proccessRunning = true;
						}else {
							threadMap.remove(entry.getKey());
						}
					}
				}
				if(!proccessRunning) {
					NativeUrlManipulator nativeUrlManipulator = new NativeUrlManipulator(ds);
					Thread thread = new Thread(nativeUrlManipulator);
					thread.start();
					threadMap.put(thread, nativeUrlManipulator);
				}
			}
		}
		RequestDispatcher view = req.getRequestDispatcher("manipulator.jsp");
		view.forward(req, resp);
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException
	{	
		doPost(req, resp);
	}

	private void stopThreads()
	{
		for(Map.Entry<Thread, Manipulator> entry : threadMap.entrySet()) {
			if(entry != null) {
				entry.getValue().stopThread();
				entry.getKey().interrupt();
			}
		}
		threadMap.clear();
	}

	@Override
	public void destroy()
	{
		super.destroy();
		stopThreads();
	}

	@Override
	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		try {
			Context ctx = new InitialContext();
			Context envctx =  (Context) ctx.lookup("java:comp/env");
			ds =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
		}catch(NamingException e) {
			e.printStackTrace();
		}
	}

}
