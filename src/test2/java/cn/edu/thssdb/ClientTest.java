package cn.edu.thssdb;

import cn.edu.thssdb.client.Client;
import cn.edu.thssdb.rpc.thrift.IService;
import cn.edu.thssdb.utils.Global;
import org.apache.commons.cli.*;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.junit.Before;
import org.junit.Test;
import org.junit.After;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ClientTest {
    private static final Logger logger = LoggerFactory.getLogger(ClientTest.class);
    static final String HOST_ARGS = "h";
    static final String HOST_NAME = "host";
    static final String HELP_ARGS = "help";
    static final String HELP_NAME = "help";
    static final String PORT_ARGS = "p";
    static final String PORT_NAME = "port";
    TTransport transport;
    private static Options createOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder(HELP_ARGS)
                        .argName(HELP_NAME)
                        .desc("Display help information(optional)")
                        .hasArg(false)
                        .required(false)
                        .build());
        options.addOption(
                Option.builder(HOST_ARGS)
                        .argName(HOST_NAME)
                        .desc("Host (optional, default 127.0.0.1)")
                        .hasArg(false)
                        .required(false)
                        .build());
        options.addOption(
                Option.builder(PORT_ARGS)
                        .argName(PORT_NAME)
                        .desc("Port (optional, default 6667)")
                        .hasArg(false)
                        .required(false)
                        .build());
        return options;
    }
    private static CommandLine parseCmd(String[] args) {
        Options options = createOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            logger.error(e.getMessage());
            System.exit(-1);
        }
        return cmd;
    }
    @Before
    public void setUp() {
        // 启动客户端
        String[] args = new String[]{};
        CommandLine commandLine = parseCmd(args);
        String host = commandLine.getOptionValue(HOST_ARGS, Global.DEFAULT_SERVER_HOST);
        int port =
                Integer.parseInt(
                        commandLine.getOptionValue(PORT_ARGS, String.valueOf(Global.DEFAULT_SERVER_PORT)));
        transport = new TSocket(host, port);
        try {
            transport.open();
        } catch (TTransportException e) {
            throw new RuntimeException(e);
        }
        TProtocol protocol = new TBinaryProtocol(transport);
        Client.client = new IService.Client(protocol);
        Client.testConnect("root","root");
    }
    @Test
    public void testFromHardCode() {
        String statement = "show databases;";
        Client.testStatement(statement);
        statement = "use testdb;";
        Client.testStatement(statement);
    }
    @Test
    public void testFromFile() {
        try {
            // 从文件中读取 SQL 语句
            File file = new File("src/test/sql/test.sql");
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                // 执行 SQL 语句
                Client.testStatement(line.trim());
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @After
    public void tearDown() {
        // 断开客户端连接
        transport.close();
    }
}