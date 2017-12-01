package org.edu_sharing.webservices.tracking;

public class TrackingProxy implements org.edu_sharing.webservices.tracking.Tracking {
  private String _endpoint = null;
  private org.edu_sharing.webservices.tracking.Tracking tracking = null;
  
  public TrackingProxy() {
    _initTrackingProxy();
  }
  
  public TrackingProxy(String endpoint) {
    _endpoint = endpoint;
    _initTrackingProxy();
  }
  
  private void _initTrackingProxy() {
    try {
      tracking = (new org.edu_sharing.webservices.tracking.TrackingServiceLocator()).getTracking();
      if (tracking != null) {
        if (_endpoint != null)
          ((javax.xml.rpc.Stub)tracking)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
        else
          _endpoint = (String)((javax.xml.rpc.Stub)tracking)._getProperty("javax.xml.rpc.service.endpoint.address");
      }
      
    }
    catch (javax.xml.rpc.ServiceException serviceException) {}
  }
  
  public String getEndpoint() {
    return _endpoint;
  }
  
  public void setEndpoint(String endpoint) {
    _endpoint = endpoint;
    if (tracking != null)
      ((javax.xml.rpc.Stub)tracking)._setProperty("javax.xml.rpc.service.endpoint.address", _endpoint);
    
  }
  
  public org.edu_sharing.webservices.tracking.Tracking getTracking() {
    if (tracking == null)
      _initTrackingProxy();
    return tracking;
  }
  
  public void trackEvent(org.edu_sharing.webservices.tracking.TrackingItem event) throws java.rmi.RemoteException, org.edu_sharing.webservices.tracking.TrackingException{
    if (tracking == null)
      _initTrackingProxy();
    tracking.trackEvent(event);
  }
  
  public void trackEvents(org.edu_sharing.webservices.tracking.TrackingItem[] events) throws java.rmi.RemoteException, org.edu_sharing.webservices.tracking.TrackingException{
    if (tracking == null)
      _initTrackingProxy();
    tracking.trackEvents(events);
  }
  
  
}