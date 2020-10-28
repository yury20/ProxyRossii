package proxy_rossii;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
public class WebServer {

    @Autowired
    private ProxyController proxyController;

    @GetMapping("/")
    public String showMainPage(Model model) {
        model.addAttribute("proxyServerMap", proxyController.getProxyServerMap());
        return "MainPage";
    }

    @GetMapping("/startProxyServer")
    public String startProxyServer(@RequestParam String proxyServerName) {
        proxyController.startProxyServer(proxyServerName);
        return "redirect:/";
    }

    @GetMapping("/stopProxyServer")
    public String stopProxyServer(@RequestParam String proxyServerName) {
        proxyController.stopProxyServer(proxyServerName);
        return "redirect:/";
    }

    @PostMapping("/setNewDelay")
    public String setNewDelay(@RequestParam String proxyServerName, @RequestParam Integer newDelay) {
        proxyController.getProxyServerMap().get(proxyServerName).getProxyData().setDelay(newDelay > 0 ? newDelay : 0);
        return "redirect:/";
    }

    @PostMapping("/addProxyServer")
    public String addProxyServer(
            @RequestParam String proxyServerName,
            @RequestParam Integer localPort,
            @RequestParam String remoteHost,
            @RequestParam Integer remotePort) {
        proxyController.addProxyServer(proxyServerName, localPort, remoteHost, remotePort);
        return "redirect:/";
    }

    @GetMapping("/deleteProxyServer")
    public String deleteProxyServer(@RequestParam String proxyServerName) {
        proxyController.stopProxyServer(proxyServerName);
        proxyController.deleteProxyServer(proxyServerName);
        return "redirect:/";
    }
}
