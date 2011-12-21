package business.uddi;

import java.io.File;
import java.rmi.RemoteException;

import org.apache.juddi.v3.client.config.UDDIClerkManager;
import org.apache.juddi.v3.client.config.UDDIClientContainer;
import org.apache.juddi.v3.client.transport.Transport;
import org.apache.juddi.v3_service.JUDDIApiPortType;
import org.uddi.api_v3.AccessPoint;
import org.uddi.api_v3.AuthToken;
import org.uddi.api_v3.BindingTemplate;
import org.uddi.api_v3.BindingTemplates;
import org.uddi.api_v3.BusinessDetail;
import org.uddi.api_v3.BusinessEntity;
import org.uddi.api_v3.BusinessService;
import org.uddi.api_v3.Description;
import org.uddi.api_v3.FindService;
import org.uddi.api_v3.FindTModel;
import org.uddi.api_v3.GetAuthToken;
import org.uddi.api_v3.Name;
import org.uddi.api_v3.SaveBinding;
import org.uddi.api_v3.SaveBusiness;
import org.uddi.api_v3.SaveService;
import org.uddi.api_v3.ServiceDetail;
import org.uddi.api_v3.ServiceInfo;
import org.uddi.api_v3.ServiceList;
import org.uddi.api_v3.TModelInstanceDetails;
import org.uddi.api_v3.TModelInstanceInfo;
import org.uddi.v3_service.DispositionReportFaultMessage;
import org.uddi.v3_service.UDDIInquiryPortType;
import org.uddi.v3_service.UDDIPublicationPortType;
import org.uddi.v3_service.UDDISecurityPortType;

/**
 * Simple Manager class that provides required methods to publish your
 * Webservices on the UDDI Server
 * 
 * 
 */
public class UddiManager {

	private JUDDIApiPortType juddiApi = null;
	private UDDISecurityPortType security = null;

	private UDDIPublicationPortType publish = null;
	private UDDIInquiryPortType inquiry = null;

	private static UddiManager instance = null;

	public static UddiManager getInstance() {
		if (instance == null) {
			instance = new UddiManager();
			instance.init();
		}
		return instance;
	}

	public void init() {
		try {

			if (security == null || juddiApi == null || publish == null) {
				String uddifile = getAbsoluteFilePath("src/business/uddi/uddi.xml");

				UDDIClerkManager manager = new UDDIClerkManager(uddifile);
				UDDIClientContainer.addClerkManager(manager);
				manager.start();
				// takes configuration from uddi.xml
				String clazz = manager.getClientConfig().getUDDINode("default")
						.getProxyTransport();
				// clazz should be:
				// org.apache.juddi.v3.client.transport.JAXWSTransport.class.getName();
				if (clazz == null || clazz.isEmpty())
					clazz = Transport.class.getName();
				Class<?> transportClass = this.getClass().getClassLoader()
						.loadClass(clazz);
				if (transportClass != null) {
					Transport transport = (Transport) transportClass
							.getConstructor(String.class)
							.newInstance("default");
					try {
						security = transport.getUDDISecurityService();
						publish = transport.getUDDIPublishService();
						inquiry = transport.getUDDIInquiryService();
						juddiApi = transport.getJUDDIApiService();
					} catch (Exception ee) {
						ee.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * retrieve the key of a tModel by its name [e.g. BOM Interface, Products
	 * and Services Interface, Order Interface]
	 * 
	 * @param name
	 *            name of tModel
	 * @param userID
	 * @return the key of a tModel
	 * @throws DispositionReportFaultMessage
	 * @throws RemoteException
	 */
	public String getTModelKeyBy(String name, String userID)
			throws DispositionReportFaultMessage, RemoteException {
		FindTModel findModel = new FindTModel();

		Name findName = new Name();
		findName.setValue(name);
		findModel.setListHead(0);
		findModel.setMaxRows(10);

		GetAuthToken getAuthTokenMyPub = new GetAuthToken();
		getAuthTokenMyPub.setUserID(userID);
		getAuthTokenMyPub.setCred("");
		AuthToken tokenPublish = getInstance().security
				.getAuthToken(getAuthTokenMyPub);

		findModel.setAuthInfo(tokenPublish.getAuthInfo());
		findModel.setName(findName);

		return getInstance().inquiry.findTModel(findModel).getTModelInfos()
				.getTModelInfo().get(0).getTModelKey();
	}

	/**
	 * publish a business entity to uddi server
	 * 
	 * @param userID
	 *            = Gruppenname;
	 * @param businessName
	 *            = Gruppenname;
	 * @return the key of the newly created business
	 * 
	 * @throws DispositionReportFaultMessage
	 * @throws RemoteException
	 */
	public String publishBusinessEntity(String userID, String businessName)
			throws DispositionReportFaultMessage, RemoteException {
		GetAuthToken getAuthTokenMyPub = new GetAuthToken();
		getAuthTokenMyPub.setUserID(userID);
		getAuthTokenMyPub.setCred("");
		org.uddi.api_v3.AuthToken myPubAuthToken = getInstance().security
				.getAuthToken(getAuthTokenMyPub);

		BusinessEntity myBusEntity = new BusinessEntity();
		Name myBusName = new Name();
		myBusName.setValue(businessName);
		myBusEntity.getName().add(myBusName);

		SaveBusiness sb = new SaveBusiness();
		sb.getBusinessEntity().add(myBusEntity);
		sb.setAuthInfo(myPubAuthToken.getAuthInfo());
		BusinessDetail bd = getInstance().publish.saveBusiness(sb);
		return bd.getBusinessEntity().get(0).getBusinessKey();
	}

	/**
	 * publish your service corresponding to a given tModel provided by the uddi
	 * server
	 * 
	 * @param tModelKey
	 *            key of tModel (-> retieve tModel byName, e.g.
	 *            "Products and Services Interface")
	 * @param userID
	 *            id of publisher (e.g. MN1, GW2,..)
	 * @param businessKey
	 *            key of the organisation publishing the service (e.g. MN1,
	 *            GW2,...)
	 * @param serviceNameStr
	 * @param serviceDescriptionStr
	 * @param wsdlLocation
	 *            location where you have publisher your wsdl (e.g.
	 *            http://140.78
	 *            .73.87:8090/MN1Services/services/ProductsAndServicesImplPort
	 *            ?wsdl)
	 * @throws DispositionReportFaultMessage
	 * @throws RemoteException
	 */
	public void publishServiceCorrespondingTo(String tModelKey, String userID,
			String businessKey, String serviceNameStr,
			String serviceDescriptionStr, String wsdlLocation)
			throws DispositionReportFaultMessage, RemoteException {
		GetAuthToken getAuthTokenMyPub = new GetAuthToken();
		getAuthTokenMyPub.setUserID(userID);
		getAuthTokenMyPub.setCred("");
		org.uddi.api_v3.AuthToken myPubAuthToken = getInstance().security
				.getAuthToken(getAuthTokenMyPub);

		BusinessService myService = new BusinessService();
		myService.setBusinessKey(businessKey);
		Name myServName = new Name();
		myServName.setValue(serviceNameStr);
		myService.getName().add(myServName);

		// Make a description
		Description serviceDescription = new Description();
		serviceDescription.setValue(serviceDescriptionStr);

		myService.getDescription().add(serviceDescription);

		// Add binding templates, etc...
		BindingTemplates templates = new BindingTemplates();
		BindingTemplate bindingTemplate = new BindingTemplate();
		bindingTemplate.getDescription().add(serviceDescription);

		// Set access point
		AccessPoint accessPoint = new AccessPoint();
		accessPoint.setUseType("wsdlDeployment");
		accessPoint.setValue(wsdlLocation);

		bindingTemplate.setAccessPoint(accessPoint);

		templates.getBindingTemplate().add(bindingTemplate);

		myService.setBindingTemplates(templates);

		// Adding the service to the "save" structure, using our publisher's
		// authentication info and
		// saving away.
		SaveService ss = new SaveService();
		ss.getBusinessService().add(myService);
		ss.setAuthInfo(myPubAuthToken.getAuthInfo());
		ServiceDetail sd = getInstance().publish.saveService(ss);
		String myServKey = sd.getBusinessService().get(0).getServiceKey();

		// Save our binding

		bindingTemplate.setServiceKey(myServKey);

		TModelInstanceInfo tmodelinstanceinfo = new TModelInstanceInfo();
		tmodelinstanceinfo.setTModelKey(tModelKey);

		TModelInstanceDetails tmodeldetails = new TModelInstanceDetails();
		tmodeldetails.getTModelInstanceInfo().add(tmodelinstanceinfo);
		bindingTemplate.setTModelInstanceDetails(tmodeldetails);

		SaveBinding saveBinding = new SaveBinding();
		saveBinding.setAuthInfo(myPubAuthToken.getAuthInfo());
		saveBinding.getBindingTemplate().add(bindingTemplate);
		getInstance().publish.saveBinding(saveBinding);

	}

	/**
	 * find service by tModel key
	 * 
	 * @param tModelName
	 * @param userID
	 * @return List of services foun
	 * @throws DispositionReportFaultMessage
	 * @throws RemoteException
	 */
	public ServiceList findServicesBy(String tModelName, String userID)
			throws DispositionReportFaultMessage, RemoteException {
		FindService find = new FindService();
		FindTModel findModel = new FindTModel();

		GetAuthToken getAuthTokenMyPub = new GetAuthToken();
		getAuthTokenMyPub.setUserID(userID);
		getAuthTokenMyPub.setCred("");
		AuthToken tokenPublish = getInstance().security
				.getAuthToken(getAuthTokenMyPub);

		Name name = new Name();
		name.setValue(tModelName);
		findModel.setAuthInfo(tokenPublish.getAuthInfo());
		findModel.setName(name);

		find.setFindTModel(findModel);
		ServiceList foundList = getInstance().inquiry.findService(find);

		return foundList;

	}

	public void print(ServiceList foundList) {
		if (foundList.getServiceInfos() != null) {
			if (foundList.getServiceInfos().getServiceInfo() != null) {
				System.out.println("Number of service infos "
						+ foundList.getServiceInfos().getServiceInfo().size()
						+ "\n");

				System.out
						.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");

				for (ServiceInfo info : foundList.getServiceInfos()
						.getServiceInfo()) {
					System.out.println("Name: "
							+ info.getName().get(0).getValue());
					System.out.println("Service Key" + info.getServiceKey());
					System.out.println("Service Business Key"
							+ info.getBusinessKey());
				}
			} else {
				System.out
						.println("\n Ooops: foundList.getServiceInfos().getServiceInfo() is null!\n");
			}
		} else {
			System.out
					.println("\n Ooops: foundList.getServiceInfos() is null!\n");
		}
	}

	private static String getAbsoluteFilePath(String fileName) {
		String result = null;
		File f = null;
		try {
			f = new File(fileName);
			result = f.getAbsolutePath();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	public static void main(String[] args) {
		// After deploying your Service to Tomcat 8085 you will have to publish
		// your service to the uddi server.
		// methods provided in this class will help you to publish the name of
		// your business entity as well as
		// publishing your service corresponding to the provided tModel.

	}
}
