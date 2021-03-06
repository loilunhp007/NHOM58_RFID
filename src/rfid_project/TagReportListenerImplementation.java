package rfid_project;

import com.impinj.octane.ImpinjReader;
import com.impinj.octane.Tag;
import com.impinj.octane.TagReport;
import com.impinj.octane.TagReportListener;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import rfid_project.DatabaseUltils.OrderDAO;
import rfid_project.DatabaseUltils.OrderDetailDAO;
import rfid_project.DatabaseUltils.ProductDAO;
import rfid_project.DatabaseUltils.TagDAO;
import rfid_project.Entity.Order;
import rfid_project.Entity.OrderDetail;
import rfid_project.Entity.Tag2;


public class TagReportListenerImplementation implements TagReportListener {

     List<Tag2> tag2s = new ArrayList<>();
    static List<Tag2> tagToUpdate = new ArrayList<>();
    static TagDAO tagdao = new TagDAO();
    static OrderDAO orderdao = new OrderDAO();
    static Order order = new Order();
    static boolean flag = false;
     OrderDetailDAO detaildao = new OrderDetailDAO();
    static Map<String, Integer> mymap = new HashMap<String, Integer>();
     ProductDAO productdao = new ProductDAO();
     List<String> tts = new ArrayList<>();
     LocalDateTime dtime = LocalDateTime.now();
     DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    static List<String> u = new ArrayList<>();
    @Override
    public void onTagReported(ImpinjReader reader, TagReport report) {
        List<Tag> tags = report.getTags();
        System.out.println("mymap:"+ mymap.size());
        for (Tag t : tags) {
            handleTag(t.getEpc().toString(),t.getAntennaPortNumber()+"");
        }
        if (mymap.isEmpty()) {
                //System.out.println("Tag không hợp lệ");
            } else {
                String pid = (String) mymap.keySet().toArray()[0];
                List<OrderDetail> dd = detaildao.getOrderDetail(pid);//orderid
                if (dd.isEmpty()) {
                    flag = true;
                    System.out.println("không có đơn xuất");
                } else {
                    OrderDetail d = dd.get(0);
                    if ((orderdao.getOrderBy(d.getOrderID()).isEmpty())) {
                        System.out.println("không có đơn xuất");
                    } else {
                        order = orderdao.getOrderBy(d.getOrderID()).get(0);
                        System.out.println("order" + order.getOrderID());
                        order.setOrderdetails(detaildao.getOrderDetail2(d.getOrderID()));
                        List<OrderDetail> details = order.getOrderdetails();
                        List<String> idd = new ArrayList();
                        for (OrderDetail orrr : details) {
                            if (!idd.contains(orrr.getProduct())) {
                                idd.add(orrr.getProductID());
                            }
                        }
                        System.out.println("detail: size"+ details.size());    
                        for (int i = 0; i < details.size(); i++) {
                            if (mymap.containsKey(details.get(i).getProductID())) {
                                System.out.println("mymap::::"+mymap.get(details.get(i).getProductID()));
                                if (mymap.get(details.get(i).getProductID()) == details.get(i).getQuantity()) {
                                    
                                } else {
                                    //read size > quan dư
                                    //read size < quan thieu
                                    System.out.println("soluong:" + mymap.get(details.get(i).getProductID()) + "expected:" + details.get(i).getQuantity());
                                    if ((details.get(i).getQuantity()) < mymap.get(details.get(i).getProductID())) {
                                        int exceed = mymap.get(details.get(i).getProductID()) - details.get(i).getQuantity();
                                        JOptionPane.showMessageDialog(null, productdao.getProductById(details.get(i).getProductID()).getName() + " dư : " + exceed);
                                    } else {
                                        int exceed = details.get(i).getQuantity() - mymap.get(details.get(i).getProductID());
                                        JOptionPane.showMessageDialog(null, productdao.getProductById(details.get(i).getProductID()).getName() + " thiếu : " + exceed);
                                    }
                                    flag = true;
                                }
                            } else {
                                flag = true;
                                System.out.println(productdao.getProductById(details.get(i).getProductID()).getName() + " thiếu " + details.get(i).getQuantity());
                            }

                        }
                    }
                }
                if (flag) {

                } else {
                    System.out.println("success");
                    Order confirmOrder = new Order();
                    confirmOrder = order;
                    System.out.println("orr" + confirmOrder.getOrderID());
                    confirmOrder.setStatus(3);
                    tagdao.updateTag2(tagToUpdate);
                    orderdao.updateOrder(confirmOrder);

                }
                mymap.clear();
                flag=false;
            }
    }

    //get number of tags
    public  void handleTag(String tagId, String analog) {
        tag2s = tagdao.getTagById(tagId);
        if (tag2s.isEmpty()) {
            if(!u.contains(tagId)){
                u.add(tagId);   
            }
            
            // System.out.println("tag  hoặc không tồn tại");
        } else {
            if (tag2s.get(0).getTag_date_out() != null) {
                System.out.println("tag đã được sử dụng");
            } else {
                if (tag2s.size() > 0) {
                    System.out.println("out:"+tag2s.get(0).getProductid());
                    tag2s.get(0).setTag_gate_out(analog);
                    tag2s.get(0).setTag_date_out(dtime.format(myFormatObj));
                    if (!tts.contains(tag2s.get(0).getTagid())) {
                        if (!mymap.containsKey(tag2s.get(0).getProductid())) {
                            mymap.put(tag2s.get(0).getProductid(), 1);
                            System.out.println(tag2s.get(0).getProductid() + ":" + mymap.get(tag2s.get(0).getProductid()));
                        } else {
                            mymap.replace(tag2s.get(0).getProductid(), mymap.get(tag2s.get(0).getProductid()) + 1);
                            System.out.println("product"+tag2s.get(0).getProductid() + " :" + mymap.get(tag2s.get(0).getProductid()));
                        }
                        if(!tagToUpdate.contains(tag2s.get(0))){
                            tagToUpdate.add(tag2s.get(0));
                            tts.add(tag2s.get(0).getTagid());
                        }
                        
                        System.out.println("EPC" + tagId);
                        //mymap.put(tag2s.get(0).getProductid(), 1);
                    } else {

                        // mymap.replace(tag2s.get(0).getProductid(), mymap.get(tag2s.get(0).getProductid()) + 1);
                    }
                } else {

                }
            }

        }

    }
}
