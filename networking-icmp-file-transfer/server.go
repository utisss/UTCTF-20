package main

import (
	"encoding/binary"
	"fmt"
	"github.com/google/gopacket"
	"github.com/google/gopacket/pcap"
	"io/ioutil"
	"net"
	"time"
)

var (
	filter       string = "icmp"
	device       string = "eth0"
	snapshot_len int32  = 1024
	promiscuous  bool   = false
	err          error
	timeout      time.Duration = 1 * time.Second
	handle       *pcap.Handle
	flag         []byte
	buff         []byte
	fragSize     int = 1024
)

func check(e error) {
	if e != nil {
		panic(e)
	}
}

func main() {
	// Read flag into memory
	flag = make([]byte, 16384)
	buff, err = ioutil.ReadFile("encoded")
	check(err)
	copy(flag[:], buff[:])

	// Listen for icmp
	handle, err = pcap.OpenLive(device, snapshot_len, promiscuous, timeout)
	check(err)
	defer handle.Close()

	// Filter
	err = handle.SetBPFFilter(filter)
	check(err)

	// Handle icmp packets
	packetSource := gopacket.NewPacketSource(handle, handle.LinkType())
	for packet := range packetSource.Packets() {
		go handlePacket(packet)
	}
}

func checksum(buf []byte) uint16 {
	sum := uint32(0)

	for ; len(buf) >= 2; buf = buf[2:] {
		sum += uint32(buf[0])<<8 | uint32(buf[1])
	}
	if len(buf) > 0 {
		sum += uint32(buf[0]) << 8
	}
	for sum > 0xffff {
		sum = (sum >> 16) + (sum & 0xffff)
	}
	csum := ^uint16(sum)
	if csum == 0 {
		csum = 0xffff
	}
	return csum
}

func handlePacket(packet gopacket.Packet) {
	// Validate packet is Ping Request
	if packet.ApplicationLayer() == nil {
		return
	}

	ipv4 := packet.NetworkLayer().LayerContents()
	if ipv4[9] != 1 {
		return
	}

	icmpP := packet.NetworkLayer().LayerPayload()
	if icmpP[0] != 8 || icmpP[1] != 0 {
		return
	}

	// Open response connection
	addr := packet.NetworkLayer().NetworkFlow().Src().String()
	conn, err := net.Dial("ip4:icmp", addr)
	if err != nil {
		return
	}

	// Header
	data := make([]byte, 8+fragSize)
	copy(data[0:4], []byte{0, 0, 0, 0}) // Type, Code, Check,sum
	copy(data[4:6], icmpP[4:6])         // Valid Identifier
	copy(data[6:8], icmpP[6:8])         // Valid Sequence Number

	// Compute and attach flag fragment
	seq := int(binary.BigEndian.Uint16(icmpP[6:8]))
	if seq < 1 {
		seq = 1
	}
	start := (seq - 1) * fragSize
	end := start + fragSize
	if start > len(flag) || end > len(flag) {
		start = 0
		end = 0
	}
	copy(data[8:], flag[start:end])

	// Add valid checksum
	csum := checksum(data)
	copy(data[2:4], []byte{uint8(csum >> 8), uint8(csum & 0xff)})

	// Ping response
	_, err = conn.Write(data)
	if err != nil {
		return
	}

	fmt.Printf("Delivered %d/16 to %s\n", seq, addr)
}
