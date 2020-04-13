# Bellrock – your phone as an anonymous BLE beacon

There are millions of smartphones people carry around that support Bluetooth Low
Energy. All these phones could be used as Bluetooth beacons, i.e. to
periodically emit a signal carrying certain information such as ID, location or
name of a place. These signals could be used by other phones to infer location,
an event one is attending or proximity of friends.

The goal of this project is to develop infrastructure which would support that
while protecting a user’s privacy by changing the emitted ID each time a device
changes its place. A server will be used to maintain the anonymity of the
clients while giving them benefits as if static client UUIDs were used.

This repository contains implementation of the Bellrock Server as described in
the
[Bellrock Master Thesis](https://augustin.zidek.eu/wp/wp-content/uploads/2015/02/Master_thesis.pdf)
and also in the paper
[Bellrock—Anonymous Proximity Beacons From Personal Devices](https://www.repository.cam.ac.uk/bitstream/handle/1810/274254/anonBLE.pdf).

## Dependencies

- Java 1.8+.
- Raw data from the [OpenCellID](https://www.opencellid.org/) database. This is
  used to make the Anonymous User ID decryption faster. You will need to
  preprocess the raw data using `celltowerapi/OpenCellIDDatabaseProcessor.java`.
- [HyperSQL](http://hsqldb.org/).
- [JUnit](https://junit.org/).
- A LaTeX installation in case you want to export simulation graphs.

## License

All of this code is licensed under the MIT license. If you use the code here,
please cite the following paper:

```
@inproceedings{zidek2018bellrock,
  title={Bellrock: Anonymous Proximity Beacons From Personal Devices},
  author={Zidek, Augustin and Tailor, Shyam and Harle, Robert},
  booktitle={2018 IEEE International Conference on Pervasive Computing and Communications (PerCom)},
  pages={1--10},
  year={2018},
  organization={IEEE}
}
```
