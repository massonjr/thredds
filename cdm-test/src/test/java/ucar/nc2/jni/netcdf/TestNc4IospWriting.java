package ucar.nc2.jni.netcdf;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.*;
import ucar.nc2.NCdumpW.WantValues;
import ucar.nc2.util.CompareNetcdf2;
import ucar.nc2.util.UnitTestCommon;
import ucar.nc2.write.Nc4ChunkingStrategyNone;
import ucar.unidata.test.util.NeedsCdmUnitTest;
import ucar.unidata.test.util.TestDir;

import java.io.*;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Test copying files to netcdf4 with FileWriter2.
 * Compare original.
 *
 * @author caron
 * @since 7/27/12
 */
public class TestNc4IospWriting {
    int countNotOK = 0;

    @Before
    public void setLibrary() {
        // Ignore this class's tests if NetCDF-4 isn't present.
        // We're using @Before because it shows these tests as being ignored.
        // @BeforeClass shows them as *non-existent*, which is not what we want.
        Assume.assumeTrue("NetCDF-4 C library not present.", Nc4Iosp.isClibraryPresent());
    }

    // @Test
    @Category(NeedsCdmUnitTest.class)
    public void problem() throws IOException {
        copyFile("Q:/cdmUnitTest/formats/netcdf4/files/xma022032.nc5", "C:/temp/xma022032.nc5", NetcdfFileWriter.Version
                .netcdf4);
        //copyFile("C:/dev/github/thredds/cdm/src/test/data/testWriteRecord.nc", "C:/temp/testWriteRecord.classic.nc3", NetcdfFileWriter.Version.netcdf3c);
    }

    @Test
    @Category(NeedsCdmUnitTest.class)
    public void writeNetcdf4Files() throws IOException {
        int count = 0;
        count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/files/", new MyFileFilter(), new MyAct(), true);
        System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
    }

    @Test
    @Category(NeedsCdmUnitTest.class)
    public void writeNetcdf4Compound() throws IOException {
        int count = 0;
        count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/compound/", new MyFileFilter(), new MyAct(), true);
        System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
    }

    // enum not ready

    //@Test
    @Category(NeedsCdmUnitTest.class)
    public void writeHdf5Samples() throws IOException {
        int count = 0;
        count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/samples/", new MyFileFilter(), new MyAct(), true);
        System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
    }

    //@Test
    @Category(NeedsCdmUnitTest.class)
    public void writeHdf5Support() throws IOException {
        int count = 0;
        count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/support/", new MyFileFilter(), new MyAct(), true);
        System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
    }

    // @Test
    @Category(NeedsCdmUnitTest.class)
    public void writeNetcdf4Tst() throws IOException {
        int count = 0;
        count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/tst/", new MyFileFilter(), new MyAct(), true);
        System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
    }


    @Test
    @Category(NeedsCdmUnitTest.class)
    public void writeNetcdf4Zender() throws IOException {
        int count = 0;
        count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf4/zender/", new MyFileFilter(), new MyAct(), true);
        System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
    }

    //@Test
    @Category(NeedsCdmUnitTest.class)
    public void readAllHDF5() throws IOException {
        int count = 0;
        count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/hdf5/", null, new MyAct(), true);
        System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
    }

    @Test
    @Category(NeedsCdmUnitTest.class)
    public void writeAllNetcdf3() throws IOException {
        int count = 0;
        count += TestDir.actOnAll(TestDir.cdmUnitTestDir + "formats/netcdf3/", null, new MyAct());
        System.out.printf("***READ %d files FAIL = %d%n", count, countNotOK);
    }

    private class MyAct implements TestDir.Act {
        public int doAct(String datasetIn) throws IOException
        {
            File fin = new File(datasetIn);
            String datasetOut = tempDir + fin.getName();

            if(!copyFile(datasetIn, datasetOut, NetcdfFileWriter.Version.netcdf4))
                countNotOK++;
            return 1;
        }
    }

    private class MyFileFilter implements FileFilter {
        @Override
        public boolean accept(File pathname)
        {
            if(pathname.getName().equals("tst_opaque_data.nc4")) return false;
            if(pathname.getName().equals("tst_opaques.nc4")) return false;
            return true;
        }
    }


    private String tempDir = TestDir.temporaryLocalDataDir; // "C:/temp/";

    private boolean copyFile(String datasetIn, String datasetOut, NetcdfFileWriter.Version version) throws IOException {

        System.out.printf("TestNc4IospWriting copy %s to %s%n", datasetIn, datasetOut);
        NetcdfFile ncfileIn = ucar.nc2.NetcdfFile.open(datasetIn, null);
        FileWriter2 writer2 = new FileWriter2(ncfileIn, datasetOut, version, null);
        NetcdfFile ncfileOut = writer2.write();
        compare(ncfileIn, ncfileOut, true, false, true);
        ncfileIn.close();
        ncfileOut.close();
        // System.out.println("NetcdfFile written = " + ncfileOut);
        return true;
    }

    private boolean compare(NetcdfFile nc1, NetcdfFile nc2, boolean showCompare, boolean showEach, boolean compareData) throws IOException {
        Formatter f = new Formatter();
        CompareNetcdf2 tc = new CompareNetcdf2(f, showCompare, showEach, compareData);
        boolean ok = tc.compare(nc1, nc2, new CompareNetcdf2.Netcdf4ObjectFilter(), showCompare, showEach, compareData);
        System.out.printf(" %s compare %s to %s ok = %s%n", ok ? "" : "***", nc1.getLocation(), nc2.getLocation(), ok);
        if(!ok) System.out.printf(" %s%n", f);
        return ok;
    }


    /////////////////////////////////////////////////

    // Demonstrates GitHub issue #191. Unignore when we have a fix in place.
    @Test
    public void writeEnumType() throws IOException {
        // NetcdfFile's 0-arg constructor is protected, so must use NetcdfFileSubclass
        NetcdfFile ncFile = new NetcdfFileSubclass();

        // Create shared, unlimited Dimension
        Dimension timeDim = new Dimension("time", 3, true, true, false);
        ncFile.addDimension(null, timeDim);

        // Create a map from integers to strings.
        Map<Integer, String> enumMap = new HashMap<>();
        enumMap.put(18, "pie");
        enumMap.put(268, "donut");
        enumMap.put(3284, "cake");

        // Create EnumTypedef and add it to root group.
        EnumTypedef dessertType = new EnumTypedef("dessertType", enumMap, DataType.ENUM2);
        ncFile.getRootGroup().addEnumeration(dessertType);

        // Create Variable of type dessertType.
        Variable dessert = new Variable(ncFile, null, null, "dessert", DataType.ENUM2, "time");
        dessert.setEnumTypedef(dessertType);

        // Add data to dessert variable.
        short[] dessertStorage = new short[]{18, 268, 3284};
        dessert.setCachedData(Array.factory(DataType.SHORT, new int[]{3}, dessertStorage), true);

        // Add the variable to the root group and finish ncFile
        ncFile.addVariable(null, dessert);
        ncFile.finish();
        ncFile.setLocation("writeEnumType");


        File outFile = File.createTempFile("writeEnumType", ".nc");
        //File outFile = new File("f:/git/thredds/writeEnumType.nc"); outFile.delete();
        try {
            FileWriter2 writer = new FileWriter2(
                    ncFile, outFile.getAbsolutePath(), NetcdfFileWriter.Version.netcdf4, new Nc4ChunkingStrategyNone());
            String mem = null;
            String disk = null;
            try (NetcdfFile ncFileOut = writer.write()) {
                ncFileOut.setLocation("writeEnumType");
                // For debugging. Delete this entire try block when done.
                Writer out = new StringWriter();
                NCdumpW.print(ncFile, out, WantValues.all, false, false, null, null);
                out.close();
                mem = out.toString();
                out = new StringWriter();
                NCdumpW.print(ncFileOut, out, WantValues.all, false, false, null, null);
                out.close();
                disk = out.toString();
                System.out.println("-------------------In memory-------------------");
                System.out.print(mem);
                System.out.println("-------------------On disk-------------------");
                System.out.print(disk);
            }
            String diffs = UnitTestCommon.compare("TestNc4IospWriting.writeEnumType", mem, disk);
            if(diffs != null) {
                System.out.println("-------------------Diffs-------------------");
                System.err.println(diffs);
                System.out.println("---------------------------------------------");
            }
            Assert.assertTrue("Differences", diffs == null);
        } finally {
            ncFile.close();
            outFile.delete();
        }
    }


    // Demonstrates GitHub issue #301--badly writing subsetted arrays
    @Test
    public void writeSubset() throws IOException, InvalidRangeException {
        String fname = TestLocal.temporaryDataDir + "writeSubset.nc";
        try (NetcdfFileWriter ncFile = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, fname)) {
            // Create shared, unlimited Dimension
            ncFile.addDimension(null, "x", 5);

            // Create a float Variable
            Variable arr = ncFile.addVariable(null, "arr", DataType.FLOAT, "x");

            // Create file and exit redefine
            ncFile.create();

            // Create an array of data and subset
            float[] data = new float[]{1.f, 2.f, 3.f, 4.f, 5.f, 6.f, 7.f, 8.f, 9.f, 10.f};
            Array arrData = Array.factory(DataType.FLOAT, new int[]{10}, data);
            Array subArr = arrData.sectionNoReduce(new int[]{1}, new int[]{5}, new int[]{2});

            // Write data to file
            ncFile.write(arr, subArr);
        }

        // Make sure file has what we expect
        try (NetcdfFile ncFile = NetcdfFile.open(fname)) {
            Variable arr = ncFile.findVariable(null, "arr");
            Assert.assertEquals(5, arr.getSize());
            Array arrData = arr.read();
            float[] data = (float[])arrData.get1DJavaArray(Float.class);
            Assert.assertEquals(5, data.length);
            Assert.assertArrayEquals(new float[]{2.f, 4.f, 6.f, 8.f, 10.f},
                    data, 1e-6f);
        }
    }
}
