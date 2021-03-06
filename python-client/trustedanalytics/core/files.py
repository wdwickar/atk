#
# Copyright (c) 2015 Intel Corporation 
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

from trustedanalytics.core.atktypes import valid_data_types

class DataFile(object):
    annotation = "data_file"
    pass


class CsvFile(DataFile):
    """
    Define a CSV file.


    Parameters
    ----------
    file_name : str
        The name of the file containing data in a CSV format.
        The file must be in the Hadoop file system.
        Relative paths are interpreted as being relative to the path set in
        the application configuration file.
        See :ref:`Configure File System Root
        <ad_inst_IA_configure_file_system_root>`.
        Absolute paths (beginning with ``hdfs://...``, for example) are also
        supported.
    schema : list of tuples of the form (string, type)
        A description of the fields of data in the form of a list of tuples,
        which describe each field.
        Each tuple is in the form (name, type), where the name is a string,
        and type is a supported data type,
        Upon import of the data, the name becomes the name of a column, so the
        names must be unique and follow column naming rules.
        For a list of valid data types, see :ref:`api_datatypes`.
        The type ``ignore`` may also be used if the field should be ignored on
        loads.
    delimiter : str (optional)
        A string which indicates the separation of the data fields.
        This is usually a single character and could be a non-visible character
        such as a tab.
        This string must be enclosed by quotes in the command declaration, for
        example ``","``.
    skip_header_lines : int (optional)
        An integer for the numbers of lines to skip before parsing records.


    Returns
    -------
    class
        A class which holds both the name and schema of a CSV file.


    Notes
    -----
    Unicode characters should not be used in the column name, because some
    functions do not support them and will not operate properly.


    Examples
    --------
    Given a raw data file named 'raw_data.csv', located at
    'hdfs://localhost.localdomain/user/trusted/data/'.
    It consists of three columns, *a*, *b*, and *c*.
    The columns have the data types *int32*, *int32*, and *str* respectively.
    The fields of data are separated by commas.
    There is no header to the file.

    Import the |PACKAGE|:

    .. code::

        >>> import trustedanalytics as ta

    Define the data:

    .. code::

        >>> csv_schema = [("a", ta.int32), ("b", ta.int32), ("c", str)]

    Create a CsvFile object with this schema:

    .. code::

        >>> csv_define = ta.CsvFile("data/raw_data.csv", csv_schema)

    The default delimiter, a comma, was used to separate fields in the file, so
    it was not specified.
    If the columns of data were separated by a character other than comma, the
    appropriate delimiter would be specified.
    For example if the data columns were separated by the colon character, the
    instruction would be:

    .. code::

        >>> ta.CsvFile("data/raw_data.csv", csv_schema, delimiter = ':')

    If the data had some lines of header at the beginning of the file, the
    lines should be skipped:

    .. code::

        >>> csv_data = ta.CsvFile("data/raw_data.csv", csv_schema, skip_header_lines=2)

    For other examples see :ref:`Importing a CSV File <example_files.csvfile>`.

    """

    # TODO - Review docstring
    annotation = "csv_file"

    def __init__(self, file_name, schema, delimiter=',', skip_header_lines=0):
        if not file_name  or not isinstance(file_name, basestring):
            raise ValueError("file_name must be a non-empty string")
        if not schema:
            raise ValueError("schema must be non-empty list of tuples")
        if not delimiter or not isinstance(delimiter, basestring):
            raise ValueError("delimiter must be a non-empty string")
        self.file_name = file_name
        self.schema = list(schema)
        self._validate()
        self.delimiter = delimiter
        self.skip_header_lines = skip_header_lines

    def __repr__(self):
        return repr(self.__dict__)

    def _schema_to_json(self):
        return [(field[0], valid_data_types.to_string(field[1]))
                for field in self.schema]

    @property
    def field_names(self):
        """
        Schema field names from the CsvFile class.


        Returns
        -------
        list
            A list of field name strings.


        Examples
        --------
        Given a raw data file 'raw_data.csv' with columns *col1* (*int32*)
        and *col2* (*float32*):

        .. only:: html

            .. code::

                >>> csv_class = ta.CsvFile("raw_data.csv", schema=[("col1", ta.int32), ("col2", ta.float32)])
                >>> print(csv_class.field_names())

        .. only:: latex

            .. code::

                >>> csv_class = ta.CsvFile("raw_data.csv",
                ... schema=[("col1", ta.int32), ("col2", ta.float32)])
                >>> print(csv_class.field_names())

        Results:

        .. code::

            ["col1", "col2"]

        """
        # TODO - Review docstring
        return [x[0] for x in self.schema]

    @property
    def field_types(self):
        """
        Schema field types from the CsvFile class.


        Returns
        -------
        list
            A list of field types.


        Examples
        --------
        Given a raw data file 'raw_data.csv' with columns *col1* (*int32*)
        and *col2* (*float32*):

        .. only:: html

            .. code::

                >>> csv_class = ta.CsvFile("raw_data.csv", schema = [("col1", ta.int32), ("col2", ta.float32)])
                >>> print(csv_class.field_types())

        .. only:: html

            .. code::

                >>> csv_class = ta.CsvFile("raw_data.csv",
                ... schema=[("col1", ta.int32), ("col2", ta.float32)])
                >>> print(csv_class.field_types())

        Results:

        .. code::

            [ta.int32, ta.float32]

        """
        # TODO - Review docstring
        return [x[1] for x in self.schema]

    def _validate(self):
        validated_schema = []
        for field in self.schema:
            name = field[0]
            if not isinstance(name, basestring):
                raise ValueError("First item in CSV schema tuple must be a string")
            try:
                data_type = valid_data_types.get_from_type(field[1])
            except ValueError:
                raise ValueError("Second item in CSV schema tuple must be a supported type: " + str(valid_data_types))
            else:
                validated_schema.append((name, data_type))
        self.schema = validated_schema

class LineFile(DataFile):
    """
    Define a line-separated file.


    Parameters
    ----------
    file_name : str
        Name of data input file.
        File must be in the Hadoop file system.
        Relative paths are interpreted relative to the
        ``trustedanalytics.atk.engine.fs.root`` configuration.
        Absolute paths (beginning with ``hdfs://...``, for example) are also
        supported.
        See :ref:`Configure File System Root
        <ad_inst_IA_configure_file_system_root>`.


    Returns
    -------
    class
        A class which holds the name of a Line File.


    Examples
    --------
    Given a raw data file 'rawline_data.txt' located at
    'hdfs://localhost.localdomain/user/trusted/data/'.
    It consists of multiple lines separated by new line character.

    Import the |PACKAGE|:

    .. code::

        >>> import trustedanalytics as ta
        >>> ta.connect()

    Define the data:

    .. code::

        >>> linefile_class = ta.LineFile("data/rawline_data.txt")

    """

    # TODO - Review docstring
    annotation = "line_file"

    def __init__(self, file_name):
        if not file_name or not isinstance(file_name, basestring):
            raise ValueError("file_name must be a non-empty string")
        self.file_name = file_name

    def __repr__(self):
        return repr(self.file_name)

class MultiLineFile(DataFile):

    annotation = "multline_file"

    def __init__(self, file_name, start_tag=None, end_tag=None):
        if not file_name or not isinstance(file_name, basestring):
            raise ValueError("file_name must be a non-empty string")
        self.file_name = file_name
        self.start_tag = start_tag
        self.end_tag = end_tag

    def __repr__(self):
        return repr(self.file_name)


class JsonFile(MultiLineFile):
    """
    Define a file as having data in JSON format.

    When JSON files are loaded into the system all top level JSON objects are
    recorded into the frame as seperate elements.


    Parameters
    ----------
    file_name : str
        Name of data input file.
        File must be in the Hadoop file system.
        Relative paths are interpreted relative to the
        ``trustedanalytics.atk.engine.fs.root`` configuration.
        Absolute paths (beginning with ``hdfs://...``, for example) are also
        supported.
        See :ref:`Configure File System Root
        <ad_inst_IA_configure_file_system_root>`.


    Returns
    -------
    class
        An object which holds both the name and tag of a JSON file.


    Examples
    --------
    Given a raw data file named 'raw_data.json' located at
    'hdfs://localhost.localdomain/user/trusted/data/'.
    It consists of a 3 top level json objects with a single value each called
    obj. Each object contains the attributes color, size, and shape.

    The example JSON file::

        { "obj": {
            "color": "blue",
            "size": 3,
            "shape": "square" }
        }
        { "obj": {
            "color": "green",
            "size": 7,
            "shape": "triangle" }
        }
        { "obj": {
            "color": "orange",
            "size": 10,
            "shape": "square" }
        }

    Import the |PACKAGE|:

    .. code::

        >>> import trustedanalytics as ta
        >>> ta.connect()

    Define the data:

    .. code::

        >>> json_file = ta.JsonFile("data/raw_data.json")

    Create a frame using this JsonFile:

    .. code::

        >>> my_frame = ta.Frame(json_file)

    The frame looks like:

    .. code::

          data_lines
        /------------------------/
          '{ "obj": {
              "color": "blue",
              "size": 3,
              "shape": "square" }
          }'
          '{ "obj": {
              "color": "green",
              "size": 7,
              "shape": "triangle" }
          }'
          '{ "obj": {
              "color": "orange",
              "size": 10,
              "shape": "square" }
          }'

    Parse values out of the XML column using the add_columns method:

    .. code::

        >>> def parse_my_json(row):
        ...     import json
        ...     my_json = json.loads(row[0])
        ...     obj = my_json['obj']
        ...     return (obj['color'], obj['size'], obj['shape'])

        >>> my_frame.add_columns(parse_my_json, [("color", str), ("size", str),
        ... ("shape", str)])

    Original XML column is no longer necessary:

    .. code::

        >>> my_frame.drop_columns(['data_lines'])

    Result:

    .. code::

        >>> my_frame.inspect()

          color:str   size:str    shape:str
        /-----------------------------------/
          blue        3           square
          green       7           triangle
          orange      10          square

    """

    annotation = "json_file"

    def __init__(self, file_name):
        if not file_name or not isinstance(file_name, basestring):
            raise ValueError("file_name must be a non-empty string")
        MultiLineFile.__init__(self, file_name, ['{'], ['}'])

    def __repr__(self):
        return repr(self.file_name)


class XmlFile(MultiLineFile):
    """
    Define an file as having data in XML format.

    When XML files are loaded into the system individual records are separated
    into the highest level elements found with the specified tag name and
    places them into a column called data_lines.


    Parameters
    ----------
    file_name : str
        Name of data input file.
        File must be in the Hadoop file system.
        Relative paths are interpreted relative to the
        ``trustedanalytics.atk.engine.fs.root`` configuration.
        Absolute paths (beginning with ``hdfs://...``, for example) are also
        supported.
        See :ref:`Configure File System Root
        <ad_inst_IA_configure_file_system_root>`.
    tag_name : str
        Tag name used to determine the split of elements into separate records.


    Returns
    -------
    class
        An object which holds both the name and tag of a XML file.


    Examples
    --------
    Given a raw data file named 'raw_data.xml' located at
    'hdfs://localhost.localdomain/user/trusted/data/'.
    It consists of a root element called *shapes* with subelements with the
    tag names *square* and *triangle*.
    Each of these subelements has two potential subelements called *name* and
    *size*.
    One of the elements has an attribute called *color*.
    Additionally, the subelement *triangle* is not needed so we can skip it
    during the import.

    The example XML file::

        <?xml version="1.0" encoding="UTF-8"?>
        <shapes>
            <square>
                <name>left</name>
                <size>3</size>
            </square>
            <triangle>
                <size>3</size>
            </triangle>
            <square color="blue">
                <name>right</name>
                <size>5</size>
            </square>
        </shapes>

    Import the |PACKAGE|:

    .. code::

        >>> import trustedanalytics as ta
        >>> ta.connect()

    Define the data:

    .. code::

        >>> xml_file = ta.XmlFile("data/raw_data.xml", "square")

    Create a frame using this XmlFile:

    .. code::

        >>> my_frame = ta.Frame(xml_file)

    The frame looks like:

    .. code::

          data_lines
        /------------------------/
          '<square>
                <name>left</name>
                <size>3</size>
           </square>'
          '<square color="blue">
                <name>right</name>
                <size>5</size>
           </square>'

    Parse values out of the XML column using the add_columns method:

    .. code::

        >>> def parse_my_xml(row):
        ...     import xml.etree.ElementTree as ET
        ...     ele = ET.fromstring(row[0])
        ...     return (ele.get("color"), ele.find("name").text, ele.find("size").text)

        >>> my_frame.add_columns(parse_my_xml, [("color", str), ("name", str), ("size", str)])

    Original XML column is no longer necessary:

    .. code::

        >>> my_frame.drop_columns(['data_lines'])

    Result:

    .. code::

        >>> my_frame.inspect()

          color:str   name:str    size:str
        /----------------------------------/
          None        left        3
          blue        right       5


    """

    annotation = "xml_file"

    def __init__(self, file_name, tag_name):
        if not file_name or not isinstance(file_name, basestring):
            raise ValueError("file_name must be a non-empty string")
        if not tag_name or not isinstance(tag_name, basestring):
            raise ValueError("tag_name is required")
        MultiLineFile.__init__(self, file_name, ['<%s>' % tag_name, '<%s ' % tag_name], ['</%s>' % tag_name])

    def __repr__(self):
        return repr(self.file_name)


class HiveQuery(DataFile):
    """
    Define the sql query to retrieve the data from a Hive table.

    Only a subset of Hive data types are supported.

    The supported data types are tinyint(cast to int), smallint(cast to int),
    int, bigint, float, double , decimal(cast to double, may lose precision), timestamp(cast to string),
    date(cast to string), string, varchar(cast to string) and boolean(cast to int)

    There is no support currently for char, arrays, maps, binary, structs and union

    Parameters
    ----------
    query : str
        The sql query to retrieve the data

    Returns
    -------
    class : HiveQuery object
        An object which holds Hive sql query.

    Examples
    --------
    Given a Hive table *person* having *name* and *age* among other columns.
    A simple query could be to get the query for the name and age
    .. code::

        >>> import trustedanalytics as ta
        >>> ta.connect()

    Define the data:

    .. code::

        >>> hive_query = ta.HiveQuery("select name, age from person")

    Create a frame using the object:

    .. code::

        >>> my_frame = ta.Frame(hive_query)

    """

    annotation = "hive_settings"

    def __init__(self, sql_query):
        if not sql_query or not isinstance(sql_query, basestring):
            raise ValueError("The query must be a non-empty string")
        self.file_name = sql_query

    def __repr__(self):
        return repr(self.file_name)
