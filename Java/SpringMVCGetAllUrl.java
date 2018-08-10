
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;


@RequestMapping(value = "getAllUrl",name = "路径")
@ResponseBody
public List<Object> getAllUrl(HttpServletRequest request) {
	List<Object> list = new ArrayList<>();
	WebApplicationContext wc = (WebApplicationContext) request.getAttribute(DispatcherServlet.WEB_APPLICATION_CONTEXT_ATTRIBUTE);
	RequestMappingHandlerMapping bean = wc.getBean(RequestMappingHandlerMapping.class);
	Map<RequestMappingInfo, HandlerMethod> handlerMethods = bean.getHandlerMethods();
	for (RequestMappingInfo rmi : handlerMethods.keySet()) {
		Map<String,Object>  result = new HashMap<>();
		PatternsRequestCondition pc = rmi.getPatternsCondition();
		/*rmi.getName();
		Set<String> pSet = pc.getPatterns();
		result.addAll(pSet);*/
		result.put(rmi.getName(),pc.getPatterns());
		list.add(result);
	}
	return list;
}
