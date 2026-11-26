// file: app/src/main/java/com/plum/dnsscanner/DnsTester.kt
package com.plum.dnsscanner

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class DnsServer(val name: String, val ip: String)
data class TestResult(val serverName: String, val serverIp: String, val successCount: Int, val totalDomains: Int, val avgTimeMs: Long)

object DnsTester {
    val dnsServers = listOf(
        DnsServer("Cloudflare", "1.1.1.1"),
        DnsServer("Google", "8.8.8.8"),
        DnsServer("Quad9", "9.9.9.9"),
        DnsServer("OpenDNS", "208.67.222.222"),
        DnsServer("Yandex", "77.88.8.8")
    )

    val domains = listOf(
        "youtu.be", "youtube.com", "i.ytimg.com", "i9.ytimg.com", "yt3.ggpht.com",
        "yt4.ggpht.com", "googleapis.com", "jnn-pa.googleapis.com", "googleusercontent.com",
        "signaler-pa.youtube.com", "youtubei.googleapis.com", "manifest.googlevideo.com",
        "yt3.googleusercontent.com", "rr1---sn-4axm-n8vs.googlevideo.com",
        "rr1---sn-gvnuxaxjvh-o8ge.googlevideo.com", "rr1---sn-ug5onuxaxjvh-p3ul.googlevideo.com",
        "rr1---sn-ug5onuxaxjvh-n8v6.googlevideo.com", "rr4---sn-q4flrnsl.googlevideo.com",
        "rr10---sn-gvnuxaxjvh-304z.googlevideo.com", "rr14---sn-n8v7kn7r.googlevideo.com",
        "rr16---sn-axq7sn76.googlevideo.com", "rr1---sn-8ph2xajvh-5xge.googlevideo.com",
        "rr1---sn-gvnuxaxjvh-5gie.googlevideo.com", "rr12---sn-gvnuxaxjvh-bvwz.googlevideo.com",
        "rr5---sn-n8v7knez.googlevideo.com", "rr1---sn-u5uuxaxjvhg0-ocje.googlevideo.com",
        "rr2---sn-q4fl6ndl.googlevideo.com", "rr5---sn-gvnuxaxjvh-n8vk.googlevideo.com",
        "rr4---sn-jvhnu5g-c35d.googlevideo.com", "rr1---sn-q4fl6n6y.googlevideo.com",
        "rr2---sn-hgn7ynek.googlevideo.com", "rr1---sn-xguxaxjvh-gufl.googlevideo.com"
    )

    private fun buildDnsQuery(domain: String): ByteArray {
        val packet = ByteArray(512)
        packet[0] = 0x12; packet[1] = 0x34
        packet[2] = 0x01; packet[3] = 0x00
        packet[4] = 0x00; packet[5] = 0x01

        var pos = 12
        for (part in domain.split(".")) {
            packet[pos++] = part.length.toByte()
            for (c in part.toCharArray()) packet[pos++] = c.code.toByte()
        }
        packet[pos++] = 0x00
        packet[pos++] = 0x00; packet[pos++] = 0x01
        packet[pos++] = 0x00; packet[pos++] = 0x01

        return packet.copyOf(pos)
    }

    fun testServer(server: DnsServer, onProgress: (Int, Int) -> Unit): TestResult {
        var successCount = 0
        var totalTime = 0L
        val total = domains.size

        for ((index, domain) in domains.withIndex()) {
            try {
                val query = buildDnsQuery(domain)
                val address = InetAddress.getByName(server.ip)
                
                DatagramSocket().use { socket ->
                    socket.soTimeout = 2000
                    val packet = DatagramPacket(query, query.size, address, 53)
                    
                    val startTime = System.currentTimeMillis()
                    socket.send(packet)
                    
                    val response = ByteArray(512)
                    val receivePacket = DatagramPacket(response, response.size)
                    socket.receive(receivePacket)
                    val endTime = System.currentTimeMillis()
                    
                    if (receivePacket.length > 12) {
                        successCount++
                        totalTime += (endTime - startTime)
                    }
                }
            } catch (e: Exception) {
                // Игнорируем таймауты и ошибки
            }
            onProgress(index + 1, total)
        }

        val avgTime = if (successCount > 0) totalTime / successCount else 0
        return TestResult(server.name, server.ip, successCount, total, avgTime)
    }
}
