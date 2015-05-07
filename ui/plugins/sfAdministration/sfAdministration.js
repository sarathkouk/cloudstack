(function (cloudStack) {
  cloudStack.plugins.sfAdministration = function(plugin) {
    plugin.ui.addSection({
      id: 'sfAdministration',
      title: 'SolidFire Administration',
      sectionSelect: {
        label: 'label.select-view',
        preFilter: function() {
          return ['sfAdministration'];
        }
      },
      preFilter: function(args) {
        return isAdmin();
      },
      sections: {
        sfAdministration: {
          id: 'sfAdministration',
          type: 'select',
          title: 'SolidFire Clusters',
          listView: {
            section: 'sfAdministration',
            id: 'sfAdministration',
            fields: {
              name: { label: 'label.name' },
              mvip: { label: 'MVIP' },
              username: { label: 'Username' },
              zonename: { label: 'label.zone' }
            },
            dataProvider: function(args) {
              plugin.ui.apiCall('listSolidFireClusters', {
                success: function(json) {
                  var sfclustersfiltered = [];
                  var sfclusters = json.listsolidfireclustersresponse.sfcluster;
                  var search = args.filterBy.search.value == null ? "" : args.filterBy.search.value.toLowerCase();

                  if (search == "") {
                    sfclustersfiltered = sfclusters;
                  }
                  else {
                    for (i = 0; i < sfclusters.length; i++) {
                      sfcluster = sfclusters[i];

                      if (sfcluster.name.toLowerCase().indexOf(search) > -1 ) {
                        sfclustersfiltered.push(sfcluster);
                      }
                    }
                  }

                  args.response.success({ data: sfclustersfiltered });
                },
                error: function(errorMessage) {
                  args.response.error(errorMessage);
                }
              });
            },
            actions: {
              add: {
                label: 'Add Reference to Cluster',
                preFilter: function(args) {
                  return true;
                },
                messages: {
                  confirm: function(args) {
                    return 'Please fill in the following data to add a reference to a cluster.';
                  },
                  notification: function(args) {
                    return 'Add Reference to Cluster';
                  }
                },
                createForm: {
                  title: 'Add Reference to Cluster',
                  desc: 'Please fill in the following data to add a reference to a cluster.',
                  fields: {
                    availabilityZone: {
                      label: 'label.availability.zone',
                      docID: 'helpVolumeAvailabilityZone',
                      validation: {
                        required: true
                      },
                      select: function(args) {
                        $.ajax({
                          url: createURL("listZones&available=true"),
                          dataType: "json",
                          async: true,
                          success: function(json) {
                            var zoneObjs = json.listzonesresponse.zone;

                            args.response.success({
                              descriptionField: 'name',
                              data: zoneObjs
                            });
                          }
                        });
                      }
                    },
                    mvip: {
                      label: 'MVIP',
                      validation: {
                        required: true
                      }
                    },
                    username: {
                      label: 'label.username',
                      validation: {
                        required: true
                      }
                    },
                    password: {
                      label: 'label.password',
                      isPassword: true,
                      validation: {
                        required: true
                      }
                    },
                    totalCapacity: {
                      label: 'Total Capacity (in GB)',
                      validation: {
                        required: true,
                        number: true
                      }
                    },
                    totalMinIops: {
                      label: 'Total Min IOPS',
                      validation: {
                        required: true,
                        number: true
                      }
                    },
                    totalMaxIops: {
                      label: 'Total Max IOPS',
                      validation: {
                        required: true,
                        number: true
                      }
                    },
                    totalBurstIops: {
                      label: 'Total Burst IOPS',
                      validation: {
                        required: true,
                        number: true
                      }
                    }
                  }
                },
                action: function(args) {
                  var data = {
                    zoneid: args.data.availabilityZone,
                    mvip: args.data.mvip,
                    username: args.data.username,
                    password: args.data.password,
                    totalcapacity: args.data.totalCapacity,
                    totalminiops: args.data.totalMinIops,
                    totalmaxiops: args.data.totalMaxIops,
                    totalburstiops: args.data.totalBurstIops
                  };

                  $.ajax({
                    url: createURL('createReferenceToSolidFireCluster'),
                    data: data,
                    success: function(json) {
                      var sfclusterObj = json.createreferencetosolidfireclusterresponse.apicreatereferencetosolidfirecluster;

                      args.response.success({
                        data: sfclusterObj
                      });
                    },
                    error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                }
              }
            },
            detailView: {
              name: 'label.details',
              viewAll: {
                path: 'sfAdministration.sfVirtualNetworks',
                label: 'Virtual Networks'
              },
              actions: {
                edit: {
                  label: 'label.edit',
                  messages: {
                    notification: function(args) {
                      return 'Edit Cluster';
                    }
                  },
                  action: function (args) {
                    var params = [];

                    params.push("&name=" + args.context.sfAdministration[0].name);
                    params.push("&totalcapacity=" + args.data.totalcapacity);
                    params.push("&totalminiops=" + args.data.totalminiops);
                    params.push("&totalmaxiops=" + args.data.totalmaxiops);
                    params.push("&totalburstiops=" + args.data.totalburstiops);

                    $.ajax({
                      url: createURL('updateReferenceToSolidFireCluster' + params.join("")),
                      success: function(json) {
                        var sfclusterObj = json.updatereferencetosolidfireclusterresponse.apiupdatereferencetosolidfirecluster;

                        args.response.success({
                          data: sfclusterObj
                        });
                      },
                      error: function(json) {
                        args.response.error(parseXMLHttpResponse(json));
                      }
                    });
                  }
                },
                remove: {
                  label: 'Delete Reference to Cluster',
                  messages: {
                    confirm: function(args) {
                      return 'Are you sure you would like to delete this reference to a SolidFire cluster?';
                    },
                    notification: function(args) {
                      return 'Delete Reference to Cluster';
                    }
                  },
                  action: function(args) {
                    $.ajax({
                      url: createURL('deleteReferenceToSolidFireCluster&name=' + args.context.sfAdministration[0].name),
                      success: function(json) {
                        args.response.success();
                      },
                      error: function(json) {
                        args.response.error(parseXMLHttpResponse(json));
                      }
                    });
                  }
                }
              },
              tabs: {
                details: {
                  title: 'label.details',
                  preFilter: function(args) {
                    return [];
                  },
                  fields: [
                    {
                      name: {
                        label: 'label.name'
                      }
                    },
                    {
                      uuid: {
                        label: 'label.id'
                      },
                      zonename: {
                        label: 'label.zone'
                      },
                      mvip: {
                        label: 'MVIP'
                      },
                      username: {
                        label: 'Username'
                      },
                      totalcapacity: {
                        label: 'Total Capacity (in GB)',
                        isEditable: true
                      },
                      totalminiops: {
                        label: 'Total Min IOPS',
                        isEditable: true
                      },
                      totalmaxiops: {
                        label: 'Total Max IOPS',
                        isEditable: true
                      },
                      totalburstiops: {
                        label: 'Total Burst IOPS',
                        isEditable: true
                      }
                    }
                  ],
                  dataProvider: function(args) {
                    $.ajax({
                      url: createURL("listSolidFireClusters&name=" + args.context.sfAdministration[0].name),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var jsonObj = json.listsolidfireclustersresponse.sfcluster[0];

                        args.response.success({
                          data: jsonObj
                        });
                      }
                    });
                  }
                }
              }
            }
          }
        },
        sfVirtualNetworks: {
          id: 'sfVirtualNetworks',
          type: 'select',
          title: 'SolidFire Virtual Networks',
          listView: {
            section: 'sfVirtualNetworks',
            id: 'sfVirtualNetworks',
            fields: {
              name: { label: 'label.name' },
              tag: { label: 'Tag' },
              svip: { label: 'SVIP' },
              accountname: { label: 'label.account' }
            },
            dataProvider: function(args) {
              var clustername = args.context.sfAdministration[0].name;

              plugin.ui.apiCall('listSolidFireVirtualNetworks&clustername=' + clustername, {
                success: function(json) {
                  var sfvirtualnetworksfiltered = [];
                  var sfvirtualnetworks = json.listsolidfirevirtualnetworksresponse.sfvirtualnetwork;
                  var search = args.filterBy.search.value == null ? "" : args.filterBy.search.value.toLowerCase();

                  if (search == "") {
                    sfvirtualnetworksfiltered = sfvirtualnetworks;
                  }
                  else {
                    for (i = 0; i < sfvirtualnetworks.length; i++) {
                      sfvirtualnetwork = sfvirtualnetworks[i];

                      if (sfvirtualnetwork.name.toLowerCase().indexOf(search) > -1 ) {
                        sfvirtualnetworksfiltered.push(sfvirtualnetwork);
                      }
                    }
                  }

                  args.response.success({ data: sfvirtualnetworksfiltered });
                },
                error: function(errorMessage) {
                  args.response.error(errorMessage);
                }
              });
            },
            actions: {
              add: {
                label: 'Add Virtual Network',
                preFilter: function(args) {
                  return true;
                },
                messages: {
                  confirm: function(args) {
                    return 'Please fill in the following data to add a virtual network.';
                  },
                  notification: function(args) {
                    return 'Add Virtual Network';
                  }
                },
                createForm: {
                  title: 'Add Virtual Network',
                  desc: 'Please fill in the following data to add a virtual network.',
                  fields: {
                    account: {
                      label: 'Account',
                      validation: {
                        required: true
                      },
                      select: function(args) {
                        $.ajax({
                          url: createURL("listAccounts&listAll=true"),
                          dataType: "json",
                          async: true,
                          success: function(json) {
                            var accountObjs = json.listaccountsresponse.account;

                            args.response.success({
                              descriptionField: 'name',
                              data: accountObjs
                            });
                          }
                        });
                      }
                    },
                    name: {
                      label: 'Name',
                      validation: {
                        required: true
                      }
                    },
                    tag: {
                      label: 'Tag',
                      validation: {
                        required: true
                      }
                    },
                    netmask: {
                      label: 'Netmask',
                      validation: {
                        required: true
                      }
                    },
                    startip: {
                      label: 'Start IP',
                      validation: {
                        required: true
                      }
                    },
                    size: {
                      label: 'Size',
                      validation: {
                        required: true,
                        number: true
                      }
                    },
                    svip: {
                      label: 'SVIP',
                      validation: {
                        required: true
                      }
                    }
                  }
                },
                action: function(args) {
                  var data = {
                    clustername: args.context.sfAdministration[0].name,
                    accountid: args.data.account,
                    name: args.data.name,
                    tag: args.data.tag,
                    netmask: args.data.netmask,
                    startip: args.data.startip,
                    size: args.data.size,
                    svip: args.data.svip
                  };

                  $.ajax({
                    url: createURL('createSolidFireVirtualNetwork'),
                    data: data,
                    success: function(json) {
                      var sfVirtualNetworkObj = json.createsolidfirevirtualnetworkresponse.apicreatesolidfirevirtualnetwork;

                      args.response.success({
                        data: sfVirtualNetworkObj
                      });
                    },
                    error: function(json) {
                      args.response.error(parseXMLHttpResponse(json));
                    }
                  });
                }
              }
            },
            detailView: {
              name: 'label.details',
              actions: {
                edit: {
                  label: 'label.edit',
                  messages: {
                    notification: function(args) {
                      return 'Edit Virtual Network';
                    }
                  },
                  action: function (args) {
                    var params = [];

                    params.push("&id=" + args.context.sfVirtualNetworks[0].id);
                    params.push("&name=" + args.data.name);
                    params.push("&netmask=" + args.data.netmask);
                    params.push("&startip=" + args.data.startip);
                    params.push("&size=" + args.data.size);

                    $.ajax({
                      url: createURL('updateSolidFireVirtualNetwork' + params.join("")),
                      success: function(json) {
                        var sfVirtualNetworkObj = json.updatesolidfirevirtualnetworkresponse.apiupdatesolidfirevirtualnetwork;

                        args.response.success({
                          data: sfVirtualNetworkObj
                        });
                      },
                      error: function(json) {
                        args.response.error(parseXMLHttpResponse(json));
                      }
                    });
                  }
                },
                remove: {
                  label: 'Delete Virtual Network',
                  messages: {
                    confirm: function(args) {
                      return 'Are you sure you would like to delete this virtual network?';
                    },
                    notification: function(args) {
                      return 'Delete Virtual Network';
                    }
                  },
                  action: function(args) {
                    $.ajax({
                      url: createURL('deleteSolidFireVirtualNetwork&id=' + args.context.sfVirtualNetworks[0].id),
                      success: function(json) {
                        args.response.success();
                      },
                      error: function(json) {
                        args.response.error(parseXMLHttpResponse(json));
                      }
                    });
                  }
                }
              },
              tabs: {
                details: {
                  title: 'label.details',
                  preFilter: function(args) {
                    return [];
                  },
                  fields: [
                    {
                      name: {
                        label: 'label.name',
                        isEditable: true
                      }
                    },
                    {
                      uuid: {
                        label: 'label.id'
                      },
                      accountname: {
                        label: 'label.account'
                      },
                      tag: {
                        label: 'Tag'
                      },
                      netmask: {
                        label: 'Netmask',
                        isEditable: true
                      },
                      startip: {
                        label: 'Start IP',
                        isEditable: true
                      },
                      size: {
                        label: 'Size',
                        number: true,
                        isEditable: true
                      },
                      svip: {
                        label: 'SVIP'
                      }
                    }
                  ],
                  dataProvider: function(args) {
                    $.ajax({
                      url: createURL("listSolidFireVirtualNetworks&id=" + args.context.sfVirtualNetworks[0].id),
                      dataType: "json",
                      async: true,
                      success: function(json) {
                        var jsonObj = json.listsolidfirevirtualnetworksresponse.sfvirtualnetwork[0];

                        args.response.success({
                          data: jsonObj
                        });
                      }
                    });
                  }
                }
              }
            }
          }
        }
      }
    });
  };
}(cloudStack));