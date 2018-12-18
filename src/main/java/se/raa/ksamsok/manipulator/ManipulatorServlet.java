package se.raa.ksamsok.manipulator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class ManipulatorServlet extends HttpServlet
{
	private static final long serialVersionUID = 213148022059727538L;
	@Autowired
	@Qualifier("dataSource")
	DataSource dataSource;

//	private DataSource dataSource = null;
//	private DataSource dsReader = null;
//	static final String DATASOURCE_NAME = "harvestdb";
//	static final String DATASOURCE_READER_NAME="harvestdbreader";
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
					NativeUrlManipulator nativeUrlManipulator = new NativeUrlManipulator(dataSource);
					String ignore = req.getParameter("ignore");
					if(ignore != null) {
						nativeUrlManipulator.setManipulateAllPosts(false);
					}
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
//		try {
//			Context ctx = new InitialContext();
//			Context envctx =  (Context) ctx.lookup("java:comp/env");
//			dataSource =  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_NAME);
//			dsReader=  (DataSource) envctx.lookup("jdbc/" + DATASOURCE_READER_NAME);
//		}catch(NamingException e) {
//			e.printStackTrace();
//		}
	}

}
